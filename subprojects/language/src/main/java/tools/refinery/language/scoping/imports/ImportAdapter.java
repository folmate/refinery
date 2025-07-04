/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.annotations.AnnotationValidator;
import tools.refinery.language.annotations.internal.CompositeAnnotationValidator;
import tools.refinery.language.expressions.CompositeTermInterpreter;
import tools.refinery.language.expressions.TermInterpreter;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.library.RefineryLibrary;
import tools.refinery.language.library.internal.CompositeLibrary;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.utils.BuiltinSymbols;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ImportAdapter extends AdapterImpl {
	private static final Logger LOG = Logger.getLogger(ImportAdapter.class);
	private static final List<Class<? extends RefineryLibrary>> DEFAULT_LIBRARIES;
	private static final List<Class<? extends TermInterpreter>> DEFAULT_TERM_INTERPRETERS;
	private static final List<Class<? extends AnnotationValidator>> DEFAULT_ANNOTATION_VALIDATORS;
	private static final List<Path> DEFAULT_PATHS;

	static {
		DEFAULT_LIBRARIES = loadServices(RefineryLibrary.class);
		DEFAULT_TERM_INTERPRETERS = loadServices(TermInterpreter.class);
		DEFAULT_ANNOTATION_VALIDATORS = loadServices(AnnotationValidator.class);
		var pathEnv = System.getenv("REFINERY_LIBRARY_PATH");
		if (pathEnv == null) {
			DEFAULT_PATHS = List.of();
		} else {
			DEFAULT_PATHS = Splitter.on(File.pathSeparatorChar)
					.splitToStream(pathEnv)
					.map(pathString -> Path.of(pathString).toAbsolutePath().normalize())
					.toList();
		}
	}

	private static <T> List<Class<? extends T>> loadServices(Class<T> serviceClass) {
		return ServiceLoader.load(serviceClass).stream()
				.<Class<? extends T>>mapMulti((provider, consumer) -> {
					Class<? extends T> implementationClass = null;
					try {
						implementationClass = provider.type();
					} catch (ServiceConfigurationError e) {
						LOG.error("Error loading service: " + serviceClass.getName(), e);
					}
					if (implementationClass != null) {
						consumer.accept(implementationClass);
					}
				})
				.toList();
	}

	private ResourceSet resourceSet;
	private final List<RefineryLibrary> libraries;
	private final List<TermInterpreter> termInterpreters;
	private final List<AnnotationValidator> annotationValidators;
	private final RefineryLibrary compositeLibrary;
	private final TermInterpreter termInterpreter;
	private final AnnotationValidator annotationValidator;
	private final List<Path> libraryPaths = new ArrayList<>(DEFAULT_PATHS);
	private final Cache<QualifiedName, QualifiedName> failedResolutions =
			CacheBuilder.newBuilder().maximumSize(100).build();
	private final Map<QualifiedName, URI> qualifiedNameToUriMap = new LinkedHashMap<>();
	private final Map<URI, QualifiedName> uriToQualifiedNameMap = new LinkedHashMap<>();
	private Problem builtinProblem;
	private BuiltinSymbols builtinSymbols;

	@Inject
	public ImportAdapter(Injector injector) {
		libraries = instantiate(injector, DEFAULT_LIBRARIES);
		compositeLibrary = new CompositeLibrary(libraries);
		termInterpreters = instantiate(injector, DEFAULT_TERM_INTERPRETERS);
		termInterpreter = new CompositeTermInterpreter(termInterpreters);
		annotationValidators = instantiate(injector, DEFAULT_ANNOTATION_VALIDATORS);
		annotationValidator = new CompositeAnnotationValidator(annotationValidators);
	}

	private <T> List<T> instantiate(Injector injector, List<Class<? extends T>> implementationClasses) {
		var instances = new ArrayList<T>(implementationClasses.size());
		for (var implementationClass : implementationClasses) {
			T instance = null;
			try {
				instance = injector.getInstance(implementationClass);
			} catch (RuntimeException e) {
				LOG.error("Error loading service: " + implementationClass.getName(), e);
			}
			if (instance != null) {
				instances.add(instance);
			}
		}
		return instances;
	}

	void setResourceSet(ResourceSet resourceSet) {
		this.resourceSet = resourceSet;
		for (var resource : resourceSet.getResources()) {
			resourceAdded(resource);
		}
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return type == ImportAdapter.class;
	}

	public List<RefineryLibrary> getLibraries() {
		return libraries;
	}

	public List<TermInterpreter> getTermInterpreters() {
		return termInterpreters;
	}

	public List<AnnotationValidator> getAnnotationValidators() {
		return annotationValidators;
	}

	public RefineryLibrary getLibrary() {
		return compositeLibrary;
	}

	public TermInterpreter getTermInterpreter() {
		return termInterpreter;
	}

	public AnnotationValidator getAnnotationValidator() {
		return annotationValidator;
	}

	public List<Path> getLibraryPaths() {
		return libraryPaths;
	}

	@Nullable
	public URI resolveAndCacheQualifiedName(QualifiedName qualifiedName) {
		var uri = getResolvedUri(qualifiedName);
		if (uri != null) {
			return uri;
		}
		if (isFailed(qualifiedName)) {
			return null;
		}
		var result = compositeLibrary.resolveQualifiedName(qualifiedName, libraryPaths);
		if (result.isPresent()) {
			uri = result.get();
			markAsResolved(qualifiedName, uri);
			return uri;
		}
		markAsUnresolved(qualifiedName);
		return null;
	}

	private URI getResolvedUri(QualifiedName qualifiedName) {
		return qualifiedNameToUriMap.get(qualifiedName);
	}

	private boolean isFailed(QualifiedName qualifiedName) {
		return failedResolutions.getIfPresent(qualifiedName) != null;
	}

	private void markAsResolved(QualifiedName qualifiedName, URI uri) {
		if (qualifiedNameToUriMap.put(qualifiedName, uri) != null) {
			throw new IllegalArgumentException("Already resolved " + qualifiedName);
		}
		// We don't need to signal an error here, because modules with multiple qualified names will lead to
		// validation errors later.
		uriToQualifiedNameMap.putIfAbsent(uri, qualifiedName);
		failedResolutions.invalidate(qualifiedName);
	}

	private void markAsUnresolved(QualifiedName qualifiedName) {
		failedResolutions.put(qualifiedName, qualifiedName);
	}

	public QualifiedName getQualifiedName(URI uri) {
		return uriToQualifiedNameMap.get(uri);
	}

	@Override
	public void notifyChanged(Notification msg) {
		switch (msg.getEventType()) {
		case Notification.ADD -> {
			if (msg.getNewValue() instanceof Resource resource) {
				resourceAdded(resource);
			}
		}
		case Notification.ADD_MANY -> {
			if (msg.getNewValue() instanceof List<?> list) {
				manyResourcesAdded(list);
			}
		}
		case Notification.REMOVE -> {
			if (msg.getOldValue() instanceof Resource resource) {
				resourceRemoved(resource);
			}
		}
		case Notification.REMOVE_MANY -> {
			if (msg.getOldValue() instanceof List<?> list) {
				manyResourcesRemoved(list);
			}
		}
		default -> {
			// Nothing to update.
		}
		}
	}

	private void manyResourcesAdded(List<?> list) {
		for (var element : list) {
			if (element instanceof Resource resource) {
				resourceAdded(resource);
			}
		}
	}

	private void manyResourcesRemoved(List<?> list) {
		for (var element : list) {
			if (element instanceof Resource resource) {
				resourceRemoved(resource);
			}
		}
	}

	private void resourceAdded(Resource resource) {
		var uri = resource.getURI();
		var result = compositeLibrary.computeQualifiedName(uri, libraryPaths);
		if (result.isEmpty()) {
			return;
		}
		var qualifiedName = result.get();
		var previousQualifiedName = uriToQualifiedNameMap.putIfAbsent(uri, qualifiedName);
		if (previousQualifiedName == null) {
			if (qualifiedNameToUriMap.put(qualifiedName, uri) != null) {
				throw new IllegalArgumentException("Duplicate resource for" + qualifiedName);
			}
		} else if (!previousQualifiedName.equals(qualifiedName)) {
			LOG.warn("Expected %s to have qualified name %s, got %s instead".formatted(
					uri, previousQualifiedName, qualifiedName));
		}
	}

	private void resourceRemoved(Resource resource) {
		var qualifiedName = uriToQualifiedNameMap.remove(resource.getURI());
		if (qualifiedName != null) {
			qualifiedNameToUriMap.remove(qualifiedName);
		}
	}

	public Problem getBuiltinProblem() {
		if (builtinProblem == null) {
			var builtinResource = resourceSet.getResource(BuiltinLibrary.BUILTIN_LIBRARY_URI, true);
			if (builtinResource == null) {
				throw new IllegalStateException("Failed to load builtin resource");
			}
			var contents = builtinResource.getContents();
			if (contents.isEmpty()) {
				throw new IllegalStateException("builtin resource is empty");
			}
			builtinProblem = (Problem) contents.getFirst();
			EcoreUtil.resolveAll(builtinResource);
		}
		return builtinProblem;
	}

	public BuiltinSymbols getBuiltinSymbols() {
		if (builtinSymbols == null) {
			builtinSymbols = new BuiltinSymbols(getBuiltinProblem());
		}
		return builtinSymbols;
	}
}
