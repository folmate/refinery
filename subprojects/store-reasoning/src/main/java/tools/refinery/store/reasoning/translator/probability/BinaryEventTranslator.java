package tools.refinery.store.reasoning.translator.probability;

import tools.refinery.logic.dnf.AbstractQueryBuilder;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.probability.terms.EventTerm;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.representation.EventRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;

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
		var output = Variable.of(EventHandle.class);
		var query = Query.builder(relation.name)
				.parameters(parameters)
				.output(output)
				.clause(
						must(selector.call(parameters)),
						output.assign(EventHandle.of(0))
				)
				.build();
		relation.query(query);
		storeBuilder.getAdapter(ModelQueryBuilder.class).queries(query);
	}

	public static NodeVariable[] createParameters(EventRelation eventRelation) {
		var parameters = new NodeVariable[eventRelation.arity()];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = Variable.of();
		}
		return parameters;
	}
}
