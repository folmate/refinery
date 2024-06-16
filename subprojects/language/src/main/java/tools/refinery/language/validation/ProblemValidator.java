/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.validation.Check;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.typesystem.ProblemTypeAnalyzer;
import tools.refinery.language.typesystem.SignatureProvider;
import tools.refinery.language.utils.ProblemUtil;

import java.util.*;

/**
 * This class contains custom validation rules.
 * <p>
 * See
 * <a href="https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation">...</a>
 */
public class ProblemValidator extends AbstractProblemValidator {
	private static final String ISSUE_PREFIX = "tools.refinery.language.validation.ProblemValidator.";
	public static final String UNEXPECTED_MODULE_NAME_ISSUE = ISSUE_PREFIX + "UNEXPECTED_MODULE_NAME";
	public static final String INVALID_IMPORT_ISSUE = ISSUE_PREFIX + "INVALID_IMPORT";
	public static final String SINGLETON_VARIABLE_ISSUE = ISSUE_PREFIX + "SINGLETON_VARIABLE";
	public static final String NODE_CONSTANT_ISSUE = ISSUE_PREFIX + "NODE_CONSTANT_ISSUE";
	public static final String DUPLICATE_NAME_ISSUE = ISSUE_PREFIX + "DUPLICATE_NAME";
	public static final String INVALID_MULTIPLICITY_ISSUE = ISSUE_PREFIX + "INVALID_MULTIPLICITY";
	public static final String ZERO_MULTIPLICITY_ISSUE = ISSUE_PREFIX + "ZERO_MULTIPLICITY";
	public static final String MISSING_OPPOSITE_ISSUE = ISSUE_PREFIX + "MISSING_OPPOSITE";
	public static final String INVALID_OPPOSITE_ISSUE = ISSUE_PREFIX + "INVALID_OPPOSITE";
	public static final String INVALID_SUPERTYPE_ISSUE = ISSUE_PREFIX + "INVALID_SUPERTYPE";
	public static final String INVALID_REFERENCE_TYPE_ISSUE = ISSUE_PREFIX + "INVALID_REFERENCE_TYPE";
	public static final String INVALID_ARITY_ISSUE = ISSUE_PREFIX + "INVALID_ARITY";
	public static final String INVALID_MODALITY_ISSUE = ISSUE_PREFIX + "INVALID_MODALITY";
	public static final String INVALID_RULE_ISSUE = ISSUE_PREFIX + "INVALID_RULE";
	public static final String INVALID_TRANSITIVE_CLOSURE_ISSUE = ISSUE_PREFIX + "INVALID_TRANSITIVE_CLOSURE";
	public static final String UNSUPPORTED_ASSERTION_ISSUE = ISSUE_PREFIX + "UNSUPPORTED_ASSERTION";
	public static final String UNKNOWN_EXPRESSION_ISSUE = ISSUE_PREFIX + "UNKNOWN_EXPRESSION";
	public static final String INVALID_ASSIGNMENT_ISSUE = ISSUE_PREFIX + "INVALID_ASSIGNMENT";
	public static final String TYPE_ERROR = ISSUE_PREFIX + "TYPE_ERROR";
	public static final String VARIABLE_WITHOUT_EXISTS = ISSUE_PREFIX + "VARIABLE_WITHOUT_EXISTS";

	@Inject
	private ReferenceCounter referenceCounter;

	@Inject
	private ExistsVariableCollector existsVariableCollector;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Inject
	private SignatureProvider signatureProvider;

	@Inject
	private ProblemTypeAnalyzer typeAnalyzer;

	@Check
	public void checkModuleName(Problem problem) {
		var nameString = problem.getName();
		if (nameString == null) {
			return;
		}
		var resource = problem.eResource();
		if (resource == null) {
			return;
		}
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			return;
		}
		var adapter = importAdapterProvider.getOrInstall(resourceSet);
		var expectedName = adapter.getQualifiedName(resource.getURI());
		if (expectedName == null) {
			return;
		}
		var name = NamingUtil.stripRootPrefix(qualifiedNameConverter.toQualifiedName(nameString));
		if (!expectedName.equals(name)) {
			var moduleKindName = switch (problem.getKind()) {
				case PROBLEM -> "problem";
				case MODULE -> "module";
			};
			var message = "Expected %s to have name '%s', got '%s' instead.".formatted(
					moduleKindName, qualifiedNameConverter.toString(expectedName),
					qualifiedNameConverter.toString(name));
			error(message, problem, ProblemPackage.Literals.NAMED_ELEMENT__NAME, INSIGNIFICANT_INDEX,
					UNEXPECTED_MODULE_NAME_ISSUE);
		}
	}

	@Check
	public void checkImportStatement(ImportStatement importStatement) {
		var importedModule = importStatement.getImportedModule();
		if (importedModule == null || importedModule.eIsProxy()) {
			return;
		}
		String message = null;
		var problem = EcoreUtil2.getContainerOfType(importStatement, Problem.class);
		if (importedModule == problem) {
			message = "A module cannot import itself.";
		} else if (importedModule.getKind() != ModuleKind.MODULE) {
			message = "Only modules can be imported.";
		}
		if (message != null) {
			error(message, importStatement, ProblemPackage.Literals.IMPORT_STATEMENT__IMPORTED_MODULE,
					INSIGNIFICANT_INDEX, INVALID_IMPORT_ISSUE);
		}
	}

	@Check
	public void checkSingletonVariable(VariableOrNodeExpr expr) {
		var variableOrNode = expr.getVariableOrNode();
		if (variableOrNode instanceof Variable variable && ProblemUtil.isImplicitVariable(variable)
				&& !ProblemUtil.isSingletonVariable(variable)) {
			if (EcoreUtil2.getContainerOfType(variable, ParametricDefinition.class) instanceof RuleDefinition &&
					EcoreUtil2.getContainerOfType(variable, NegationExpr.class) == null &&
					// If there is an exists constraint, it is the only constraint.
					existsVariableCollector.missingExistsConstraint(variable)) {
				// Existentially quantified variables in rules should not be singletons,
				// because we have to add an {@code exists} constraint as well.
				return;
			}
			var problem = EcoreUtil2.getContainerOfType(variable, Problem.class);
			if (problem != null && referenceCounter.countReferences(problem, variable) <= 1) {
				var name = variable.getName();
				var message = ("Variable '%s' has only a single reference. " +
						"Add another reference or mark is as a singleton variable: '_%s'").formatted(name, name);
				warning(message, expr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
						INSIGNIFICANT_INDEX, SINGLETON_VARIABLE_ISSUE);
			}
		}
	}

	@Check
	public void checkNodeConstants(VariableOrNodeExpr expr) {
		var variableOrNode = expr.getVariableOrNode();
		if (variableOrNode instanceof Node node && !ProblemUtil.isAtomNode(node)) {
			var name = node.getName();
			var message = ("Only atoms can be referenced in predicates. " +
					"Mark '%s' as an atom with the declaration 'atom %s.'").formatted(name, name);
			error(message, expr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
					INSIGNIFICANT_INDEX, NODE_CONSTANT_ISSUE);
		}
	}

	@Check
	public void checkNodeAssertionArgumentConstants(NodeAssertionArgument argument) {
		var rule = EcoreUtil2.getContainerOfType(argument, RuleDefinition.class);
		if (rule == null) {
			return;
		}
		var variableOrNode = argument.getNode();
		if (variableOrNode instanceof Node node && !ProblemUtil.isAtomNode(node)) {
			var name = node.getName();
			var message = ("Only atoms can be referenced in rule actions. " +
					"Mark '%s' as an atom with the declaration 'atom %s.'").formatted(name, name);
			error(message, argument, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE,
					INSIGNIFICANT_INDEX, NODE_CONSTANT_ISSUE);
		}
	}

	@Check
	public void checkUniqueDeclarations(Problem problem) {
		var relations = new ArrayList<NamedElement>();
		var nodes = new ArrayList<Node>();
		var aggregators = new ArrayList<AggregatorDeclaration>();
		for (var statement : problem.getStatements()) {
			if (statement instanceof Relation relation) {
				relations.add(relation);
			} else if (statement instanceof RuleDefinition ruleDefinition) {
				// Rule definitions and predicates live in the same namespace.
				relations.add(ruleDefinition);
			} else if (statement instanceof NodeDeclaration nodeDeclaration) {
				nodes.addAll(nodeDeclaration.getNodes());
			} else if (statement instanceof AggregatorDeclaration aggregatorDeclaration) {
				aggregators.add(aggregatorDeclaration);
			}
		}
		checkUniqueSimpleNames(relations);
		checkUniqueSimpleNames(nodes);
		checkUniqueSimpleNames(aggregators);
	}

	@Check
	public void checkUniqueFeatures(ClassDeclaration classDeclaration) {
		checkUniqueSimpleNames(classDeclaration.getFeatureDeclarations());
	}

	@Check
	public void checkUniqueLiterals(EnumDeclaration enumDeclaration) {
		checkUniqueSimpleNames(enumDeclaration.getLiterals());
	}

	protected void checkUniqueSimpleNames(Iterable<? extends NamedElement> namedElements) {
		var names = new LinkedHashMap<String, Set<NamedElement>>();
		for (var namedElement : namedElements) {
			var name = namedElement.getName();
			var objectsWithName = names.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
			objectsWithName.add(namedElement);
		}
		for (var entry : names.entrySet()) {
			var objectsWithName = entry.getValue();
			if (objectsWithName.size() <= 1) {
				continue;
			}
			var name = entry.getKey();
			var message = "Duplicate name '%s'.".formatted(name);
			for (var namedElement : objectsWithName) {
				acceptError(message, namedElement, ProblemPackage.Literals.NAMED_ELEMENT__NAME, 0,
						DUPLICATE_NAME_ISSUE);
			}
		}
	}

	@Check
	public void checkRangeMultiplicity(RangeMultiplicity rangeMultiplicity) {
		int lower = rangeMultiplicity.getLowerBound();
		int upper = rangeMultiplicity.getUpperBound();
		if (upper >= 0 && lower > upper) {
			var message = "Multiplicity range [%d..%d] is inconsistent.";
			acceptError(message, rangeMultiplicity, null, 0, INVALID_MULTIPLICITY_ISSUE);
		}
	}

	@Check
	public void checkReferenceMultiplicity(ReferenceDeclaration referenceDeclaration) {
		var multiplicity = referenceDeclaration.getMultiplicity();
		if (multiplicity == null) {
			return;
		}
		if (ProblemUtil.isContainerReference(referenceDeclaration) && (
				!(multiplicity instanceof RangeMultiplicity rangeMultiplicity) ||
						rangeMultiplicity.getLowerBound() != 0 ||
						rangeMultiplicity.getUpperBound() != 1)) {
			var message = "The only allowed multiplicity for container references is [0..1]";
			acceptError(message, multiplicity, null, 0, INVALID_MULTIPLICITY_ISSUE);
		}
		if ((multiplicity instanceof ExactMultiplicity exactMultiplicity &&
				exactMultiplicity.getExactValue() == 0) ||
				(multiplicity instanceof RangeMultiplicity rangeMultiplicity &&
						rangeMultiplicity.getLowerBound() == 0 &&
						rangeMultiplicity.getUpperBound() == 0)) {
			var message = "The multiplicity constraint does not allow any reference links";
			acceptWarning(message, multiplicity, null, 0, ZERO_MULTIPLICITY_ISSUE);
		}
	}

	@Check
	public void checkOpposite(ReferenceDeclaration referenceDeclaration) {
		var opposite = referenceDeclaration.getOpposite();
		if (opposite == null || opposite.eIsProxy()) {
			return;
		}
		var oppositeOfOpposite = opposite.getOpposite();
		if (oppositeOfOpposite == null) {
			acceptError("Reference '%s' does not declare '%s' as an opposite."
							.formatted(opposite.getName(), referenceDeclaration.getName()),
					referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE, 0,
					INVALID_OPPOSITE_ISSUE);
			var oppositeResource = opposite.eResource();
			if (oppositeResource != null && oppositeResource.equals(referenceDeclaration.eResource())) {
				acceptError("Missing opposite '%s' for reference '%s'."
								.formatted(referenceDeclaration.getName(), opposite.getName()),
						opposite, ProblemPackage.Literals.NAMED_ELEMENT__NAME, 0, MISSING_OPPOSITE_ISSUE);
			}
			return;
		}
		if (!referenceDeclaration.equals(oppositeOfOpposite)) {
			var messageBuilder = new StringBuilder()
					.append("Expected reference '")
					.append(opposite.getName())
					.append("' to have opposite '")
					.append(referenceDeclaration.getName())
					.append("'");
			var oppositeOfOppositeName = oppositeOfOpposite.getName();
			if (oppositeOfOppositeName != null) {
				messageBuilder.append(", got '")
						.append(oppositeOfOppositeName)
						.append("' instead");
			}
			messageBuilder.append(".");
			acceptError(messageBuilder.toString(), referenceDeclaration,
					ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE, 0, INVALID_OPPOSITE_ISSUE);
		}
	}

	@Check
	public void checkContainerOpposite(ReferenceDeclaration referenceDeclaration) {
		var kind = referenceDeclaration.getKind();
		var opposite = referenceDeclaration.getOpposite();
		if (opposite != null && opposite.eIsProxy()) {
			// If {@code opposite} is a proxy, we have already emitted a linker error.
			return;
		}
		if (kind == ReferenceKind.CONTAINMENT) {
			if (opposite != null && opposite.getKind() == ReferenceKind.CONTAINMENT) {
				acceptError("Opposite '%s' of containment reference '%s' is not a container reference."
								.formatted(opposite.getName(), referenceDeclaration.getName()),
						referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE, 0,
						INVALID_OPPOSITE_ISSUE);
			}
		} else if (kind == ReferenceKind.CONTAINER) {
			if (opposite == null) {
				acceptError("Container reference '%s' requires an opposite.".formatted(referenceDeclaration.getName()),
						referenceDeclaration, ProblemPackage.Literals.NAMED_ELEMENT__NAME, 0, MISSING_OPPOSITE_ISSUE);
			} else if (opposite.getKind() != ReferenceKind.CONTAINMENT) {
				acceptError("Opposite '%s' of container reference '%s' is not a containment reference."
								.formatted(opposite.getName(), referenceDeclaration.getName()),
						referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE, 0,
						INVALID_OPPOSITE_ISSUE);
			}
		} else if (kind == ReferenceKind.PARTIAL && opposite != null && opposite.getKind() != ReferenceKind.PARTIAL) {
			acceptError("Opposite '%s' of partial reference '%s' is not a partial reference."
							.formatted(opposite.getName(), referenceDeclaration.getName()),
					referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE, 0,
					INVALID_OPPOSITE_ISSUE);
		}
	}

	@Check
	public void checkSupertypes(ClassDeclaration classDeclaration) {
		var supertypes = classDeclaration.getSuperTypes();
		int supertypeCount = supertypes.size();
		for (int i = 0; i < supertypeCount; i++) {
			var supertype = supertypes.get(i);
			if (!supertype.eIsProxy() && !(supertype instanceof ClassDeclaration)) {
				var message = "Supertype '%s' of '%s' is not a class."
						.formatted(supertype.getName(), classDeclaration.getName());
				acceptError(message, classDeclaration, ProblemPackage.Literals.CLASS_DECLARATION__SUPER_TYPES, i,
						INVALID_SUPERTYPE_ISSUE);
			}
		}
	}

	@Check
	public void checkReferenceType(ReferenceDeclaration referenceDeclaration) {
		boolean isDefaultReference = referenceDeclaration.getKind() == ReferenceKind.DEFAULT &&
				!ProblemUtil.isContainerReference(referenceDeclaration);
		if (isDefaultReference || referenceDeclaration.getKind() == ReferenceKind.REFERENCE) {
			checkArity(referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__REFERENCE_TYPE, 1);
			return;
		}
		var referenceType = referenceDeclaration.getReferenceType();
		if (referenceType == null || referenceType.eIsProxy() || referenceType instanceof ClassDeclaration) {
			// Either correct, or a missing reference type where we are probably already emitting another error.
			return;
		}
		var message = "Reference type '%s' of the containment or container reference '%s' is not a class."
				.formatted(referenceType.getName(), referenceDeclaration.getName());
		acceptError(message, referenceDeclaration, ProblemPackage.Literals.REFERENCE_DECLARATION__REFERENCE_TYPE, 0,
				INVALID_REFERENCE_TYPE_ISSUE);
	}

	@Check
	public void checkParameter(Parameter parameter) {
		checkArity(parameter, ProblemPackage.Literals.PARAMETER__PARAMETER_TYPE, 1);
		var parametricDefinition = EcoreUtil2.getContainerOfType(parameter, ParametricDefinition.class);
		if (parametricDefinition instanceof RuleDefinition rule) {
			if (parameter.getParameterType() != null && parameter.getModality() == Modality.NONE) {
				acceptError("Parameter type modality must be specified.", parameter,
						ProblemPackage.Literals.PARAMETER__PARAMETER_TYPE, 0, INVALID_MODALITY_ISSUE);
			}
			var kind = rule.getKind();
			var binding = parameter.getBinding();
			if (kind == RuleKind.PROPAGATION && binding != ParameterBinding.SINGLE) {
				acceptError("Parameter binding annotations are not supported in propagation rules.", parameter,
						ProblemPackage.Literals.PARAMETER__BINDING, 0, INVALID_MODALITY_ISSUE);
			} else if (kind != RuleKind.DECISION && binding == ParameterBinding.MULTI) {
				acceptError("Explicit multi-object bindings are only supported in decision rules.", parameter,
						ProblemPackage.Literals.PARAMETER__BINDING, 0, INVALID_MODALITY_ISSUE);
			}
		} else {
			if (parameter.getConcreteness() != Concreteness.PARTIAL || parameter.getModality() != Modality.NONE) {
				acceptError("Modal parameter types are only supported in rule definitions.", parameter, null, 0,
						INVALID_MODALITY_ISSUE);
			}
			if (parameter.getBinding() != ParameterBinding.SINGLE) {
				acceptError("Parameter binding annotations are only supported in decision rules.", parameter,
						ProblemPackage.PARAMETER__BINDING, 0, INVALID_MODALITY_ISSUE);
			}
		}
	}

	@Check
	public void checkAtom(Atom atom) {
		int argumentCount = atom.getArguments().size();
		checkArity(atom, ProblemPackage.Literals.ATOM__RELATION, argumentCount);
		if (atom.isTransitiveClosure() && argumentCount != 2) {
			var message = "Transitive closure needs exactly 2 arguments, got %d arguments instead."
					.formatted(argumentCount);
			acceptError(message, atom, ProblemPackage.Literals.ATOM__TRANSITIVE_CLOSURE, 0,
					INVALID_TRANSITIVE_CLOSURE_ISSUE);
		}
	}

	@Check
	public void checkRuleDefinition(RuleDefinition ruleDefinition) {
		if (ruleDefinition.getKind() != RuleKind.REFINEMENT && ruleDefinition.getPreconditions().isEmpty()) {
			acceptError("Decision and propagation rules must have at least one precondition.", ruleDefinition,
					ProblemPackage.Literals.NAMED_ELEMENT__NAME, 0, INVALID_RULE_ISSUE);
		}
		if (ruleDefinition.getConsequents().size() != 1) {
			acceptError("Rules must have exactly one consequent.", ruleDefinition,
					ProblemPackage.Literals.NAMED_ELEMENT__NAME, 0, INVALID_RULE_ISSUE);
		}
		var unquantifiedVariables = new HashSet<Variable>();
		for (var variable : EcoreUtil2.getAllContentsOfType(ruleDefinition, Variable.class)) {
			if (existsVariableCollector.missingExistsConstraint(variable)) {
				unquantifiedVariables.add(variable);
			}
		}
		for (var expr : EcoreUtil2.getAllContentsOfType(ruleDefinition, VariableOrNodeExpr.class)) {
			if (expr.getVariableOrNode() instanceof Variable variable && unquantifiedVariables.contains(variable)) {
				unquantifiedVariables.remove(variable);
				var name = variable.getName();
				String message;
				if (ProblemUtil.isSingletonVariable(variable)) {
					message = ("Remove the singleton variable marker '_' and clarify the quantification of variable " +
                            "'%s'.").formatted(name);
				} else {
					message = ("Add a 'must exists(%s)', 'may exists(%s)', or 'may !exists(%s)' constraint to " +
							"clarify the quantification of variable '%s'.").formatted(name, name, name, name);
				}
				acceptWarning(message, expr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE, 0,
						VARIABLE_WITHOUT_EXISTS);
			}
		}
	}

	@Check
	public void checkAssertion(AbstractAssertion assertion) {
		var relation = assertion.getRelation();
		if (relation instanceof DatatypeDeclaration) {
			var message = "Assertions for data types are not supported.";
			acceptError(message, assertion, ProblemPackage.Literals.ABSTRACT_ASSERTION__RELATION, 0,
					UNSUPPORTED_ASSERTION_ISSUE);
			return;
		}
		int argumentCount = assertion.getArguments().size();
		checkArity(assertion, ProblemPackage.Literals.ABSTRACT_ASSERTION__RELATION, argumentCount);
	}

	@Check
	public void checkTypeScope(TypeScope typeScope) {
		checkArity(typeScope, ProblemPackage.Literals.TYPE_SCOPE__TARGET_TYPE, 1);
		if (typeScope.isIncrement() && ProblemUtil.isInModule(typeScope)) {
			acceptError("Incremental type scopes are not supported in modules", typeScope, null, 0,
					INVALID_MULTIPLICITY_ISSUE);
		}
	}

	private void checkArity(EObject eObject, EReference reference, int expectedArity) {
		var value = eObject.eGet(reference);
		if (!(value instanceof Relation relation) || relation.eIsProxy()) {
			// Feature does not point to a {@link Relation}, we are probably already emitting another error.
			return;
		}
		int arity = signatureProvider.getArity(relation);
		if (arity == expectedArity) {
			return;
		}
		var message = "Expected symbol '%s' to have arity %d, got arity %d instead."
				.formatted(relation.getName(), expectedArity, arity);
		acceptError(message, eObject, reference, 0, INVALID_ARITY_ISSUE);
	}

	@Check
	public void checkMultiObjectAssertion(Assertion assertion) {
		var builtinSymbols = importAdapterProvider.getBuiltinSymbols(assertion);
		var relation = assertion.getRelation();
		boolean isExists = builtinSymbols.exists().equals(relation);
		boolean isEquals = builtinSymbols.equals().equals(relation);
		if ((!isExists && !isEquals) || !(assertion.getValue() instanceof LogicConstant logicConstant)) {
			return;
		}
		var value = logicConstant.getLogicValue();
		if (assertion.isDefault()) {
			acceptError("Default assertions for 'exists' and 'equals' are not supported.", assertion,
					ProblemPackage.Literals.ASSERTION__DEFAULT, 0, UNSUPPORTED_ASSERTION_ISSUE);
			return;
		}
		if (value == LogicValue.ERROR) {
			acceptError("Error assertions for 'exists' and 'equals' are not supported.", assertion,
					ProblemPackage.Literals.ASSERTION__DEFAULT, 0, UNSUPPORTED_ASSERTION_ISSUE);
			return;
		}
		if (isExists) {
			checkExistsAssertion(assertion, value);
			return;
		}
		checkEqualsAssertion(assertion, value);
	}

	private void checkExistsAssertion(Assertion assertion, LogicValue value) {
		if (value == LogicValue.UNKNOWN) {
			// {@code unknown} values may always be refined to {@code true} of {@code false} if necessary (e.g., for
			// atom nodes or removed multi-objects).
			return;
		}
		var arguments = assertion.getArguments();
		if (arguments.isEmpty()) {
			// We already report an error on invalid arity.
			return;
		}
		var node = getNodeArgumentForMultiObjectAssertion(arguments.getFirst());
		if (node == null || node.eIsProxy()) {
			return;
		}
		if (ProblemUtil.isAtomNode(node) && value != LogicValue.TRUE) {
			acceptError("Atom nodes must exist.", assertion, null, 0, UNSUPPORTED_ASSERTION_ISSUE);
		}
		if (ProblemUtil.isMultiNode(node) && value != LogicValue.FALSE && ProblemUtil.isInModule(node)) {
			acceptError("Multi-objects in modules cannot be required to exist.", assertion, null, 0,
					UNSUPPORTED_ASSERTION_ISSUE);
		}
	}

	private void checkEqualsAssertion(Assertion assertion, LogicValue value) {
		var arguments = assertion.getArguments();
		if (arguments.size() < 2) {
			// We already report an error on invalid arity.
			return;
		}
		var left = getNodeArgumentForMultiObjectAssertion(arguments.get(0));
		var right = getNodeArgumentForMultiObjectAssertion(arguments.get(1));
		if (left == null || left.eIsProxy() || right == null || right.eIsProxy()) {
			return;
		}
		if (left.equals(right)) {
			if (value == LogicValue.FALSE || value == LogicValue.ERROR) {
				acceptError("A node cannot be necessarily unequal to itself.", assertion, null, 0,
						UNSUPPORTED_ASSERTION_ISSUE);
			}
		} else {
			if (value != LogicValue.FALSE) {
				acceptError("Equalities between distinct nodes are not supported.", assertion, null, 0,
						UNSUPPORTED_ASSERTION_ISSUE);
			}
		}
	}

	@Nullable
	private Node getNodeArgumentForMultiObjectAssertion(AssertionArgument argument) {
		return switch (argument) {
			case null -> null;
			case WildcardAssertionArgument ignoredWildcardAssertionArgument -> {
				acceptError("Wildcard arguments for 'exists' are not supported.", argument, null, 0,
						UNSUPPORTED_ASSERTION_ISSUE);
				yield null;
			}
			case NodeAssertionArgument nodeAssertionArgument ->
					nodeAssertionArgument.getNode() instanceof Node node ? node : null;
			default -> throw new IllegalArgumentException("Unknown assertion argument: " + argument);
		};
	}

	@Check
	private void checkImplicitNodeInModule(Assertion assertion) {
		if (!ProblemUtil.isInModule(assertion)) {
			return;
		}
		for (var argument : assertion.getArguments()) {
			if (argument instanceof NodeAssertionArgument nodeAssertionArgument) {
				var variableOrNode = nodeAssertionArgument.getNode();
				if (variableOrNode instanceof Node node &&
						!variableOrNode.eIsProxy() &&
						ProblemUtil.isImplicitNode(node)) {
					var name = node.getName();
					var message = ("Implicit nodes are not allowed in modules. Explicitly declare '%s' as a node " +
							"with the declaration 'declare %s.'").formatted(name, name);
					acceptError(message, nodeAssertionArgument, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE,
							0, UNSUPPORTED_ASSERTION_ISSUE);
				}
			}
		}
	}

	@Check
	private void checkAssignmentExpr(AssignmentExpr assignmentExpr) {
		var left = assignmentExpr.getLeft();
		if (left == null) {
			// Syntactically invalid, so we already emit an error.
			return;
		}
		if (!(left instanceof VariableOrNodeExpr variableOrNodeExpr)) {
			var message = "Left side of an assignment must be variable.";
			acceptError(message, assignmentExpr, ProblemPackage.Literals.BINARY_EXPR__LEFT,
					0, INVALID_ASSIGNMENT_ISSUE);
			return;
		}
		var target = variableOrNodeExpr.getVariableOrNode();
		if (target == null) {
			// Syntactically invalid, so we already emit an error.
			return;
		}
		if (target instanceof Parameter) {
			var message = "Parameters cannot be assigned.";
			acceptError(message, variableOrNodeExpr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
					0, INVALID_ASSIGNMENT_ISSUE);
		}
		if (target instanceof Node) {
			var message = "Nodes cannot be assigned.";
			acceptError(message, variableOrNodeExpr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
					0, INVALID_ASSIGNMENT_ISSUE);
		}
		if (!(assignmentExpr.eContainer() instanceof Conjunction)) {
			var message = "Assignments may only appear as top-level expressions.";
			acceptError(message, assignmentExpr, null, 0, INVALID_ASSIGNMENT_ISSUE);
		}
	}

	@Check
	private void checkInfiniteConstant(InfiniteConstant infiniteConstant) {
		if (!(infiniteConstant.eContainer() instanceof RangeExpr)) {
			var message = "Negative and positive infinity literals may only appear in '..' range expressions.";
			acceptError(message, infiniteConstant, null, 0, TYPE_ERROR);
		}
	}

	@Check
	private void checkTypes(Problem problem) {
		var diagnostics = typeAnalyzer.getOrComputeTypes(problem).getDiagnostics();
		for (var diagnostic : diagnostics) {
			switch (diagnostic.getSeverity()) {
			case Diagnostic.INFO -> info(diagnostic.getMessage(), diagnostic.getSourceEObject(),
					diagnostic.getFeature(), diagnostic.getIndex(), diagnostic.getIssueCode(),
					diagnostic.getIssueData());
			case Diagnostic.WARNING -> warning(diagnostic.getMessage(), diagnostic.getSourceEObject(),
					diagnostic.getFeature(), diagnostic.getIndex(), diagnostic.getIssueCode(),
					diagnostic.getIssueData());
			case Diagnostic.ERROR -> error(diagnostic.getMessage(), diagnostic.getSourceEObject(),
					diagnostic.getFeature(), diagnostic.getIndex(), diagnostic.getIssueCode(),
					diagnostic.getIssueData());
			default -> throw new IllegalStateException("Unknown severity %s of %s"
					.formatted(diagnostic.getSeverity(), diagnostic));
			}
		}
	}
}
