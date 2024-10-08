/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.utils;

import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;

import java.util.List;
import java.util.stream.Stream;

public record WrappedProblem(Problem problem) {
	public Problem get() {
		return problem;
	}

	public List<Diagnostic> getResourceErrors() {
		return problem.eResource().getErrors();
	}

	public List<Diagnostic> getResourceWarnings() {
		return problem.eResource().getWarnings();
	}

	public List<org.eclipse.emf.common.util.Diagnostic> validate() {
		return Diagnostician.INSTANCE.validate(problem).getChildren();
	}

	public WrappedProblem builtin() {
		var resourceSet = problem.eResource().getResourceSet();
		var builtinResource = resourceSet.getResource(BuiltinLibrary.BUILTIN_LIBRARY_URI, true);
		EcoreUtil.resolveAll(builtinResource);
		var builtinProblem = (Problem) builtinResource.getContents().getFirst();
		return new WrappedProblem(builtinProblem);
	}

	public List<String> nodeNames() {
		return problem.getNodes().stream().map(Node::getName).toList();
	}

	public WrappedPredicateDefinition pred(String name) {
		return new WrappedPredicateDefinition(namedStatementOfType(PredicateDefinition.class, name));
	}

	public WrappedRuleDefinition rule(String name) {
		return new WrappedRuleDefinition(namedStatementOfType(RuleDefinition.class, name));
	}

	public WrappedClassDeclaration findClass(String name) {
		return new WrappedClassDeclaration(namedStatementOfType(ClassDeclaration.class, name));
	}

	public WrappedEnumDeclaration findEnum(String name) {
		return new WrappedEnumDeclaration(namedStatementOfType(EnumDeclaration.class, name));
	}

	public WrappedAssertion assertion(int i) {
		return new WrappedAssertion(nthStatementOfType(Assertion.class, i));
	}

	public Node node(String name) {
		return ProblemNavigationUtil.named(problem.getNodes(), name);
	}

	public Node atomNode(String name) {
		var uniqueNodes = statementsOfType(NodeDeclaration.class)
				.filter(declaration -> declaration.getKind() == NodeKind.ATOM)
				.flatMap(declaration -> declaration.getNodes().stream());
		return ProblemNavigationUtil.named(uniqueNodes, name);
	}

	private <T extends Statement> Stream<T> statementsOfType(Class<? extends T> type) {
		return problem.getStatements().stream().filter(type::isInstance).map(type::cast);
	}

	private <T extends Statement & NamedElement> T namedStatementOfType(Class<? extends T> type, String name) {
		return ProblemNavigationUtil.named(statementsOfType(type), name);
	}

	private <T extends Statement> T nthStatementOfType(Class<? extends T> type, int n) {
		return statementsOfType(type).skip(n).findFirst().orElseThrow();
	}
}
