/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import tools.refinery.language.model.problem.*;

import java.util.*;

@Singleton
public class DerivedVariableComputer {
	@Inject
	private LinkingHelper linkingHelper;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider scopeProvider;

	public void installDerivedVariables(Problem problem) {
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof ParametricDefinition definition) {
				installDerivedParametricDefinitionState(definition);
			}
		}
	}

	protected void installDerivedParametricDefinitionState(ParametricDefinition definition) {
		var parameters = definition.getParameters();
		Set<String> knownVariables = HashSet.newHashSet(parameters.size());
		for (Parameter parameter : parameters) {
			String name = parameter.getName();
			if (name != null) {
				knownVariables.add(name);
			}
		}
		switch (definition) {
		case PredicateDefinition predicateDefinition ->
				installDerivedPredicateDefinitionState(predicateDefinition, knownVariables);
		case FunctionDefinition functionDefinition ->
				installDerivedFunctionDefinitionState(functionDefinition, knownVariables);
		case RuleDefinition ruleDefinition -> installDerivedRuleDefinitionState(ruleDefinition, knownVariables);
		case AnnotationDeclaration ignoredAnnotationDeclaration -> {
			// No derived state to install.
		}
		default -> throw new IllegalArgumentException("Unknown ParametricDefinition: " + definition);
		}
	}

	protected void installDerivedPredicateDefinitionState(PredicateDefinition definition, Set<String> knownVariables) {
		for (Conjunction body : definition.getBodies()) {
			createVariablesForScope(new ImplicitVariableScope(body, knownVariables));
		}
	}

	protected void installDerivedFunctionDefinitionState(FunctionDefinition definition, Set<String> knownVariables) {
		for (Case body : definition.getCases()) {
			switch (body) {
			case Conjunction conjunction ->
					createVariablesForScope(new ImplicitVariableScope(conjunction, knownVariables));
			case Match match -> {
				var condition = match.getCondition();
				if (condition != null) {
					createVariablesForScope(new ImplicitVariableScope(match, match.getCondition(), knownVariables));
				}
			}
			default -> throw new IllegalArgumentException("Unknown Case: " + body);
			}
		}
	}

	protected void installDerivedRuleDefinitionState(RuleDefinition definition, Set<String> knownVariables) {
		for (Conjunction precondition : definition.getPreconditions()) {
			createVariablesForScope(new ImplicitVariableScope(precondition, knownVariables));
		}
	}

	protected void createVariablesForScope(ImplicitVariableScope scope) {
		var queue = new ArrayDeque<ImplicitVariableScope>();
		queue.addLast(scope);
		while (!queue.isEmpty()) {
			var nextScope = queue.removeFirst();
			nextScope.createVariables(scopeProvider, linkingHelper, qualifiedNameConverter, queue);
		}
	}

	public void discardDerivedVariables(Problem problem) {
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof ParametricDefinition parametricDefinition) {
				discardParametricDefinitionState(parametricDefinition);
			}
		}
	}

	protected void discardParametricDefinitionState(ParametricDefinition definition) {
		List<ExistentialQuantifier> existentialQuantifiers = new ArrayList<>();
		List<VariableOrNodeExpr> variableOrNodeExprs = new ArrayList<>();
		var treeIterator = definition.eAllContents();
		// We must collect the nodes where we are discarding derived state and only discard them after the iteration,
		// because modifying the containment hierarchy during iteration causes the TreeIterator to fail with
		// IndexOutOfBoundsException.
		while (treeIterator.hasNext()) {
			var child = treeIterator.next();
			var containingFeature = child.eContainingFeature();
			if (containingFeature == ProblemPackage.Literals.EXISTENTIAL_QUANTIFIER__IMPLICIT_VARIABLES ||
					containingFeature == ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__SINGLETON_VARIABLE) {
				treeIterator.prune();
			} else if (child instanceof ExistentialQuantifier existentialQuantifier &&
					!existentialQuantifier.getImplicitVariables().isEmpty()) {
				existentialQuantifiers.add(existentialQuantifier);
			} else if (child instanceof VariableOrNodeExpr variableOrNodeExpr &&
					variableOrNodeExpr.getSingletonVariable() != null) {
				variableOrNodeExprs.add(variableOrNodeExpr);
			}
		}
		for (var existentialQuantifier : existentialQuantifiers) {
			existentialQuantifier.getImplicitVariables().clear();
		}
		for (var variableOrNodeExpr : variableOrNodeExprs) {
			variableOrNodeExpr.setSingletonVariable(null);
		}
	}
}
