/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ScopeDeclaration;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.web.semantics.metadata.MetadataCreator;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.util.CancellationToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class SemanticsWorker implements Callable<SemanticsResult> {
	private static final String DIAGNOSTIC_ID = "tools.refinery.language.semantics.SemanticError";

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	@Inject
	private ModelSemanticsFactory semanticsFactory;

	@Inject
	private MetadataCreator metadataCreator;

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	private Problem problem;

	private CancellationToken cancellationToken;

	public void setProblem(Problem problem, CancelIndicator parentIndicator) {
		this.problem = problem;
		cancellationToken = () -> {
			if (Thread.interrupted() || parentIndicator.isCanceled()) {
				operationCanceledManager.throwOperationCanceledException();
			}
		};
	}

	@Override
	public SemanticsResult call() {
		cancellationToken.checkCancelled();
		ModelSemantics semantics;
		try {
			semantics = semanticsFactory.cancellationToken(cancellationToken).tryCreateSemantics(problem);
		} catch (TranslationException e) {
			return new SemanticsResult(e.getMessage());
		} catch (TracedException e) {
			var cause = e.getCause();
			// Suppress the type of the cause exception.
			var message = cause == null ? e.getMessage() : cause.getMessage();
			return getTracedErrorResult(e.getSourceElement(), message);
		}
		cancellationToken.checkCancelled();
		var modelResult = createSemanticsModelResult(semantics);
		return createSemanticsResult(modelResult, semantics.getPropagationResult());
	}


	private SemanticsResult getTracedErrorResult(EObject sourceElement, String message) {
		if (sourceElement == null || !problem.eResource().equals(sourceElement.eResource())) {
			return new SemanticsResult(message);
		}
		var diagnostic = new FeatureBasedDiagnostic(Diagnostic.ERROR, message, sourceElement, null, 0,
				CheckType.EXPENSIVE, DIAGNOSTIC_ID);
		var issues = convertIssues(List.of(diagnostic));
		return new SemanticsResult(issues);
	}

	private List<ValidationResult.Issue> convertIssues(List<FeatureBasedDiagnostic> diagnostics) {
		var xtextIssues = new ArrayList<Issue>();
		for (var diagnostic : diagnostics) {
			diagnosticConverter.convertValidatorDiagnostic(diagnostic, xtextIssues::add);
		}
		return xtextIssues.stream()
				.map(issue -> new ValidationResult.Issue(issue.getMessage(), "error", issue.getLineNumber(),
						issue.getColumn(), issue.getOffset(), issue.getLength()))
				.toList();
	}

	private SemanticsModelResult createSemanticsModelResult(ModelSemantics semantics) {
		metadataCreator.setProblemTrace(semantics.getProblemTrace());
		var nodesMetadata = metadataCreator.getNodesMetadata(semantics.getModel(), Concreteness.PARTIAL);
		cancellationToken.checkCancelled();
		var relationsMetadata = metadataCreator.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(semantics, cancellationToken);
		return new SemanticsModelResult(nodesMetadata, relationsMetadata, partialInterpretation);
	}

	private SemanticsResult createSemanticsResult(SemanticsModelResult modelResult,
                                                  PropagationResult propagationResult) {
		if (!(propagationResult instanceof PropagationRejectedResult rejectedResult)) {
			return new SemanticsResult(modelResult);
		}
		var message = rejectedResult.formatMessage();
		if (!(rejectedResult.reason() instanceof ScopePropagator)) {
			return new SemanticsResult(modelResult, message);
		}
		var diagnostics = new ArrayList<FeatureBasedDiagnostic>();
		for (var statement : problem.getStatements()) {
			if (statement instanceof ScopeDeclaration scopeDeclaration) {
				diagnostics.add(new FeatureBasedDiagnostic(Diagnostic.ERROR, message, scopeDeclaration, null, 0,
						CheckType.EXPENSIVE, DIAGNOSTIC_ID));
			}
		}
		if (diagnostics.isEmpty()) {
			return new SemanticsResult(modelResult, message);
		}
		var issues = convertIssues(diagnostics);
		return new SemanticsResult(modelResult, issues);
	}
}
