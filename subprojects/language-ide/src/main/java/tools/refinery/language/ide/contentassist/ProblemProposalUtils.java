/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.contentassist;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.documentation.DocumentationCommentParser;
import tools.refinery.language.documentation.TypeHashProvider;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.utils.ProblemUtil;

@Singleton
public class ProblemProposalUtils {
	private static final String DATATYPE_KIND = "datatype";

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private DocumentationCommentParser documentationCommentParser;

	@Inject
	private TypeHashProvider typeHashProvider;

	@Nullable
	public String getDescription(IEObjectDescription candidate) {
		if (candidate.getUserData(ProblemResourceDescriptionStrategy.SHADOWING_KEY) == null) {
			// Description was not generated by our ResourceDescriptionStrategy, so we must trigger proxy resolution.
			return getDescription(candidate.getEObjectOrProxy());
		}
		if (ProblemPackage.Literals.DATATYPE_DECLARATION.isSuperTypeOf(candidate.getEClass())) {
			// Datatypes shouldn't have their arity displayed.
			return DATATYPE_KIND;
		}
		int arity = -1;
		var arityString = candidate.getUserData(ProblemResourceDescriptionStrategy.ARITY);
		try {
			arity = Integer.parseInt(arityString, 10);
		} catch (NumberFormatException e) {
			// Ignore parse error, omit arity.
		}
		var eClassDescription = getEClassDescription(candidate, null);
		return getDescriptionWithArity(arity, eClassDescription);
	}

	@Nullable
	public String getDescription(EObject eObject) {
		if (eObject instanceof DatatypeDeclaration) {
			// Datatypes shouldn't have their arity displayed.
			return DATATYPE_KIND;
		}
		int arity = eObject instanceof Relation relation ? ProblemUtil.getArityWithoutProxyResolution(relation) : -1;
		var eClassDescription = getEClassDescription(null, eObject);
		return getDescriptionWithArity(arity, eClassDescription);
	}

	private static String getDescriptionWithArity(int arity, String eClassDescription) {
		if (arity < 0) {
			return eClassDescription;
		}
		if (eClassDescription == null) {
			return "/" + arity;
		}
		return "/" + arity + " " + eClassDescription;
	}

	private static String getEClassDescription(IEObjectDescription candidate, EObject eObject) {
		var eClass = getEClass(candidate, eObject);
		if (eClass == null) {
			return null;
		}
		if (ProblemPackage.Literals.PROBLEM.isSuperTypeOf(eClass)) {
			// In contexts where we are auto-completing a Problem name, its type is obvious.
			return null;
		}
		if (ProblemPackage.Literals.NODE.isSuperTypeOf(eClass)) {
			return getNodeDescription(candidate, eObject);
		}
		if (ProblemPackage.Literals.PARAMETER.isSuperTypeOf(eClass)) {
			// Parameter must come before Variable, because it is a subclass of Variable.
			return "parameter";
		}
		if (ProblemPackage.Literals.VARIABLE.isSuperTypeOf(eClass)) {
			return "variable";
		}
		if (ProblemPackage.Literals.PREDICATE_DEFINITION.isSuperTypeOf(eClass)) {
			return getPredicateDescription(candidate, eObject);
		}
		if (ProblemPackage.Literals.CLASS_DECLARATION.isSuperTypeOf(eClass)) {
			return "class";
		}
		if (ProblemPackage.Literals.REFERENCE_DECLARATION.isSuperTypeOf(eClass)) {
			// For predicates, there is no need to show the exact type of definition, since they behave
			// logically equivalently.
			return null;
		}
		if (ProblemPackage.Literals.ENUM_DECLARATION.isSuperTypeOf(eClass)) {
			return "enum";
		}
		if (ProblemPackage.Literals.RULE_DEFINITION.isSuperTypeOf(eClass)) {
			return "rule";
		}
		if (ProblemPackage.Literals.AGGREGATOR_DECLARATION.isSuperTypeOf(eClass)) {
			return "aggregator";
		}
		if (ProblemPackage.Literals.ANNOTATION_DECLARATION.isSuperTypeOf(eClass)) {
			// In contexts where we are auto-completing an annotation name, its type is obvious.
			return null;
		}
		return eClass.getName();
	}

	private static @NotNull String getNodeDescription(IEObjectDescription candidate, EObject eObject) {
		if (isAtom(candidate, eObject)) {
			return "atom";
		}
		if (isMulti(candidate, eObject)) {
			return "mutli";
		}
		return "node";
	}

	private static boolean isAtom(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return ProblemResourceDescriptionStrategy.ATOM_TRUE.equals(
					candidate.getUserData(ProblemResourceDescriptionStrategy.ATOM));
		}
		return eObject instanceof Node node && ProblemUtil.isAtomNode(node);
	}

	private static boolean isMulti(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return ProblemResourceDescriptionStrategy.MULTI_TRUE.equals(
					candidate.getUserData(ProblemResourceDescriptionStrategy.MULTI));
		}
		return eObject instanceof Node node && ProblemUtil.isMultiNode(node);
	}

	private static @Nullable String getPredicateDescription(IEObjectDescription candidate, EObject eObject) {
		return isShadow(candidate, eObject) ? "shadow" : null;
	}

	private static boolean isShadow(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return ProblemResourceDescriptionStrategy.SHADOW_PREDICATE_TRUE.equals(
					candidate.getUserData(ProblemResourceDescriptionStrategy.SHADOW_PREDICATE));
		}
		return ProblemUtil.isShadow(eObject);
	}

	@Nullable
	public String getDocumentation(IEObjectDescription candidate, ContentAssistContext context) {
		if (ProblemPackage.Literals.PROBLEM.isSuperTypeOf(candidate.getEClass())) {
			var name = NamingUtil.stripRootPrefix(candidate.getQualifiedName());
			return getProblemDocumentation(context.getResource(), name);
		}
		if (candidate.getUserData(ProblemResourceDescriptionStrategy.SHADOWING_KEY) == null) {
			// Description was not generated by our ResourceDescriptionStrategy, so we must trigger proxy resolution.
			return getDocumentation(candidate.getEObjectOrProxy());
		}
		return candidate.getUserData(DocumentationCommentParser.DOCUMENTATION);
	}

	@Nullable
	public String getDocumentation(EObject eObject) {
		if (eObject instanceof Problem) {
			var name = NamingUtil.stripRootPrefix(qualifiedNameProvider.getFullyQualifiedName(eObject));
			return getProblemDocumentation(eObject.eResource(), name);
		}
		return documentationCommentParser.getDocumentation(eObject);
	}

	@Nullable
	private String getProblemDocumentation(Resource resource, QualifiedName name) {
		if (name == null) {
			return null;
		}
		var importAdapter = importAdapterProvider.getOrInstall(resource);
		return importAdapter.getLibrary().getDocumentation(name).orElse(null);
	}

	@NotNull
	public String[] getKind(IEObjectDescription candidate) {
		if (candidate.getUserData(ProblemResourceDescriptionStrategy.SHADOWING_KEY) == null) {
			// Description was not generated by our ResourceDescriptionStrategy, so we must trigger proxy resolution.
			return getKind(candidate.getEObjectOrProxy());
		}
		return getKind(candidate, null);
	}

	@NotNull
	public String[] getKind(EObject eObject) {
		return getKind(null, eObject);
	}

	@NotNull
	public String[] getKind(IEObjectDescription candidate, EObject eObject) {
		var eClass = getEClass(candidate, eObject);
		if (eClass == null) {
			return new String[]{ContentAssistEntry.KIND_REFERENCE};
		}
		var builder = ImmutableList.<String>builder();
		builder.add(getEClassKind(eClass));
		if (isBuiltIn(candidate, eObject) && !ProblemPackage.Literals.ANNOTATION_DECLARATION.isSuperTypeOf(eClass)) {
			// Built-in annotations are not rendered with the keyword color in the frontend.
			builder.add("builtin");
			return builder.build().toArray(new String[0]);
		}
		if (isContainment(candidate, eObject)) {
			builder.add("containment");
		}
		if (isAbstract(candidate, eObject)) {
			builder.add("abstract");
		}
		if (ProblemPackage.Literals.RELATION.isSuperTypeOf(eClass) &&
				getEObject(candidate, eObject) instanceof Relation relation) {
			var typeHash = typeHashProvider.getTypeHash(relation);
			if (typeHash != null) {
				builder.add("typeHash-" + typeHash);
			}
		}
		return builder.build().toArray(new String[0]);
	}

	private static EClass getEClass(IEObjectDescription candidate, EObject eObject) {
		EClass eClass = null;
		if (candidate != null) {
			eClass = candidate.getEClass();
		} else if (eObject != null) {
			eClass = eObject.eClass();
		}
		return eClass;
	}

	private static String getEClassKind(EClass eClass) {
		if (ProblemPackage.Literals.VARIABLE.isSuperTypeOf(eClass)) {
			return "variable";
		}
		if (ProblemPackage.Literals.NODE.isSuperTypeOf(eClass)) {
			return "node";
		}
		if (ProblemPackage.Literals.ANNOTATION_DECLARATION.isSuperTypeOf(eClass)) {
			return "annotation";
		}
		if (ProblemPackage.Literals.DATATYPE_DECLARATION.isSuperTypeOf(eClass)) {
			return DATATYPE_KIND;
		}
		if (ProblemPackage.Literals.RELATION.isSuperTypeOf(eClass)) {
			return "relation";
		}
		if (ProblemPackage.Literals.PROBLEM.isSuperTypeOf(eClass)) {
			return "module";
		}
		return ContentAssistEntry.KIND_REFERENCE;
	}

	private static boolean isBuiltIn(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return BuiltinLibrary.BUILTIN_LIBRARY_URI.equals(candidate.getEObjectURI().trimFragment());
		}
		return ProblemUtil.isBuiltIn(eObject);
	}

	private static boolean isContainment(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return ProblemResourceDescriptionStrategy.CONTAINMENT_TRUE.equals(
					candidate.getUserData(ProblemResourceDescriptionStrategy.CONTAINMENT));
		}
		return eObject instanceof ReferenceDeclaration referenceDeclaration &&
				ProblemUtil.isContainmentReference(referenceDeclaration);
	}

	private static EObject getEObject(IEObjectDescription candidate, EObject eObject) {
		return eObject == null ? candidate.getEObjectOrProxy() : eObject;
	}

	private static boolean isAbstract(IEObjectDescription candidate, EObject eObject) {
		if (candidate != null) {
			return ProblemResourceDescriptionStrategy.ABSTRACT_TRUE.equals(
					candidate.getUserData(ProblemResourceDescriptionStrategy.ABSTRACT));
		}
		return eObject instanceof ClassDeclaration classDeclaration && classDeclaration.isAbstract();
	}
}