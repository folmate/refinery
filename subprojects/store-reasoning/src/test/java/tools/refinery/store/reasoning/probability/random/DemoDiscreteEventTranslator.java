package tools.refinery.store.reasoning.probability.random;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.Arrays;
import java.util.HashMap;

public class DemoDiscreteEventTranslator implements ModelStoreConfiguration {
	private final DemoDiscreteEventRelation discreteEventRelation;
	private RelationalQuery query;

	private DemoDiscreteEventTranslator(DemoDiscreteEventRelation discreteEventRelation){
		double missingProbability = 1;
		int countImplicitTokens = 0;
		for(DemoDiscreteEventToken token : discreteEventRelation.tokens()){
			if(token.implicit()){
				countImplicitTokens++;
			} else {
				missingProbability -= token.probability();
			}
		}
		DemoDiscreteEventToken[] token = new DemoDiscreteEventToken[discreteEventRelation.tokens().length];
		for (int i=0; i<token.length; i++){
			DemoDiscreteEventToken base = discreteEventRelation.tokens()[i];
			token[i] = new DemoDiscreteEventToken(base.name(), base.implicit()?
					missingProbability/countImplicitTokens : base.probability(), false);
		}
		this.discreteEventRelation = new DemoDiscreteEventRelation(discreteEventRelation.name(),
				discreteEventRelation.arity(), token);
	}


	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var output = Variable.of(DiscreteEventHandle.class);
		var outcomes = new HashMap<String, Double>();
		Arrays.stream(discreteEventRelation.tokens()).forEach(token -> outcomes.put(token.name(), token.probability()));
		var parameters = createParameters();


		var lifter = new DnfLifter();
		var foo = lifter.lift(Modality.MUST, Concreteness.PARTIAL, query);
		var event = Query.builder(discreteEventRelation.name())
				.parameters(parameters)
				.output(output)
				.clause(
						//must(query.call(parameters)),
						foo.call(parameters),
						output.assign(DiscreteEventHandle.of(outcomes))
				)
				.build();


		storeBuilder.getAdapter(ModelQueryBuilder.class).queries(foo, event);
	}
	private NodeVariable[] createParameters() {
		var parameters = new NodeVariable[discreteEventRelation.arity()];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = Variable.of();
		}
		return parameters;
	}

	public DemoDiscreteEventTranslator query(RelationalQuery query){
		this.query = query;
		return this;
	}
	public static DemoDiscreteEventTranslator of(DemoDiscreteEventRelation discreteEventRelation){
		return new DemoDiscreteEventTranslator(discreteEventRelation);
	}
}
