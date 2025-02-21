package tools.refinery.language.semantics.internal.query;

import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.validation.ReferenceCounter;
import tools.refinery.logic.dnf.AbstractQueryBuilder;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.ConstantLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.probability.operation.EventOrAggregator;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.probability.terms.EventTerm;
import tools.refinery.probability.terms.SelectEventTerm;
import tools.refinery.store.reasoning.representation.event.DiscreteEventRelation;

import java.util.*;

public class EventCompiler extends QueryCompiler {
	public Pair<FunctionalQuery<EventHandle>,FunctionalQuery<EventHandle>> buildQuery(String name,
																					  PredicateDefinition predicateDefinition){
		try{
			var problemParameters = predicateDefinition.getParameters();
			int arity = problemParameters.size();
			var parameters = new NodeVariable[arity];


			var unaggregated = buildUnaggregatedQuery(name, predicateDefinition);

			var output = Variable.of(EventHandle.class);
			var builder = Query.builder(name).output(output).parameters(parameters);
			builder.clause(
					unaggregated.call(parameters).toLiteral(Variable.of(EventHandle.class)),
					output.assign(unaggregated.aggregate(new EventOrAggregator(), parameters))
			);
			return Tuples.create(builder.build(), unaggregated);
		} catch (TracedException e){
			e.printStackTrace();
			throw e;
		}

	}

	public FunctionalQuery<EventHandle> buildUnaggregatedQuery(String name, PredicateDefinition predicateDefinition){
		var problemParameters = predicateDefinition.getParameters();
		int arity = problemParameters.size();
		var parameters = new NodeVariable[arity];
		var parameterMap = HashMap.<tools.refinery.language.model.problem.Variable, Variable>newHashMap(arity);
		var commonLiterals = new ArrayList<Literal>();
		for (int i = 0; i < arity; i++) {
			var problemParameter = problemParameters.get(i);
			var parameter = Variable.of(problemParameter.getName());
			parameters[i] = parameter;
			parameterMap.put(problemParameter, parameter);
			var parameterType = problemParameter.getParameterType();
			if (parameterType != null) {
				var partialType = getPartialRelation(parameterType);
				commonLiterals.add(partialType.call(parameter));
			}
		}
		var builder = Query.builder(name).parameters(parameters);
		var output = Variable.of(EventHandle.class);
		for (var body : predicateDefinition.getBodies()) {
			buildConjunction(body, parameterMap, commonLiterals, builder, output);
		}
		return null;// builder.build();
	}

	void buildConjunction(
			Conjunction body, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> parameterMap,
			List<Literal> commonLiterals, AbstractQueryBuilder<?> builder, DataVariable<EventHandle> output) {
		try {
			var localScope = extendScope(parameterMap, body.getImplicitVariables());
			var problemLiterals = body.getLiterals();
			var literals = new ArrayList<>(commonLiterals);
			var events = new ArrayList<DataVariable<EventHandle>>();
			for (var problemLiteral : problemLiterals) {
				toLiteralsTraced(problemLiteral, localScope, literals, events);
			}
			//TODO EVENT add event operator builder here
			//TODO EVENT  output.assign something
			output.assign(EventTerm.AND(events.toArray(new DataVariable[0])));

			builder.clause(literals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(body, e);
		}
	}

	private void toLiteralsTraced(
			Expr expr, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals, List<DataVariable<EventHandle>> events) {
		try {
			toLiterals(expr, localScope, literals, events);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(expr, e);
		}
	}

	void toLiterals(
			Expr expr, Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals, List<DataVariable<EventHandle>> events) {

		var extractedOuter = ExtractedModalExpr.of(expr);
		var outerModality = extractedOuter.modality();

		if(expr instanceof Atom atom && atom.getEvent()!=null){
			//TODO EVENT do event stuff
			//FIXME This only works for basic events and numbers
			//FIXME For proper operation it is required that the numbers are assigned to the match
			//TODO EVENT ignore transitive closure

			var relation = atom.getRelation();
			//var constraint = getPartialRelation(relation);
			if (problemTrace.getEventTrace().get(relation) instanceof DiscreteEventRelation discrete){
				var query = discrete.query();
				var basic = Variable.of(DiscreteEventHandle.class);
				var out = Variable.of(EventHandle.class);

				var foo = atom.getEvent().getTokenSet();
				String[] specifiedOutcomes = new String[foo.size()];
				for(int i=0; i<specifiedOutcomes.length; i++){
					specifiedOutcomes[i] = foo.get(i).getName();
				}

				var argumentList = toArgumentList(atom, atom.getArguments(), localScope, literals);
				literals.add(query.call(argumentList).toLiteral(basic));
				literals.add(out.assign(
						new SelectEventTerm(basic, atom.getEvent().getCriteria()==EventCriteria.NIN,
								specifiedOutcomes)
				));

				events.add(out);
			}
		} else {
			super.toLiterals(expr, localScope, literals);
		}
	}

	private Map<tools.refinery.language.model.problem.Variable, ? extends Variable> extendScope(
			Map<tools.refinery.language.model.problem.Variable, ? extends Variable> existing,
			Collection<? extends tools.refinery.language.model.problem.Variable> newVariables) {
		if (newVariables.isEmpty()) {
			return existing;
		}
		int localScopeSize = existing.size() + newVariables.size();
		var localScope = HashMap.<tools.refinery.language.model.problem.Variable, Variable>newHashMap(localScopeSize);
		localScope.putAll(existing);
		for (var newVariable : newVariables) {
			localScope.put(newVariable, Variable.of(newVariable.getName()));
		}
		return localScope;
	}

	protected List<NodeVariable> toArgumentList(
			Expr atom, List<Expr> expressions,
			Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope,
			List<Literal> literals) {
		var arguments = new ArrayList<NodeVariable>(expressions.size());
		var referenceCounts = ReferenceCounter.computeReferenceCounts(atom);
		for (var expr : expressions) {
			if (!(expr instanceof VariableOrNodeExpr variableOrNodeExpr)) {
				throw new TracedException(expr, "Unsupported argument");
			}
			var variableOrNode = variableOrNodeExpr.getVariableOrNode();
			switch (variableOrNode) {
			case Node node -> {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(semanticsUtils.getNameWithoutRootPrefix(node).orElse("_" + nodeId));
				literals.add(new ConstantLiteral(tempVariable, nodeId));
				arguments.add(tempVariable);
			}
			case tools.refinery.language.model.problem.Variable problemVariable -> {
				if (isEffectivelySingleton(problemVariable, referenceCounts)) {
					arguments.add(Variable.of(problemVariable.getName()));
				} else {
					var variable = localScope.get(problemVariable);
					if (variable == null) {
						throw new TracedException(variableOrNode, "Unknown variable: " + problemVariable.getName());
					}
					arguments.add((NodeVariable) variable);
				}
			}
			default -> throw new TracedException(variableOrNode, "Unknown argument");
			}
		}
		return arguments;
	}

}
