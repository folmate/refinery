package tools.refinery.store.reasoning.translator.probability;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;

@Deprecated
public class CompoundEventTranslator implements ModelStoreConfiguration {
	private final FunctionalQuery<EventHandle>[] queries;
	public CompoundEventTranslator(FunctionalQuery<EventHandle>... queries){
		this.queries = queries;
	}
	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ModelQueryBuilder.class).queries(queries);
	}
}
