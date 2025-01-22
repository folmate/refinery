package tools.refinery.store.reasoning.translator.probability;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.representation.event.EventRelation;
import tools.refinery.store.reasoning.representation.event.DiscreteEventRelation;

import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class BinaryEventTranslator implements ModelStoreConfiguration {
	private final EventRelation relation;
	private final RelationalQuery selector;

	public BinaryEventTranslator(EventRelation eventRelation, RelationalQuery selector){
		this.relation = eventRelation;
		this.selector = selector;
	}
	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var parameters = createParameters(relation);
		if(relation instanceof DiscreteEventRelation discreteEventRelation){
			var output = Variable.of(DiscreteEventHandle.class);
			var outcomes = discreteEventRelation.getOutcomes();
			var query = Query.builder(relation.name)
					.parameters(parameters)
					.output(output)
					.clause(
							must(selector.call(parameters)),
							output.assign(DiscreteEventHandle.of(outcomes))
					)
					.build();
			relation.query(query);
			storeBuilder.getAdapter(ModelQueryBuilder.class).queries(query);
			return;
		} else {
			throw new IllegalStateException("Unsupported event query: "+relation.name);
		}
	}

	public static NodeVariable[] createParameters(EventRelation eventRelation) {
		var parameters = new NodeVariable[eventRelation.arity()];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = Variable.of();
		}
		return parameters;
	}
}
