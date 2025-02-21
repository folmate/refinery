package tools.refinery.store.reasoning.probability;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.probability.random.DemoDiscreteEventToken;
import tools.refinery.store.reasoning.probability.random.DemoDiscreteEventTranslator;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class DiscreteEventTest {
	@Test
	public void discreteEventTest(){
		var component = new PartialRelation("Component", 1);
		var dependsOn = new PartialRelation("dependsOn", 2);
		var operational = new DemoDiscreteEventRelation("operational", 1,
				new DemoDiscreteEventToken("OP",0.9,false),
				new DemoDiscreteEventToken("INOP",0.1,false));

		var componentStorage = Symbol.of("Component", 1, TruthValue.class, TruthValue.FALSE);
		var dependsOnStorage = Symbol.of("dependsOn", 2, TruthValue.class, TruthValue.UNKNOWN);

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(component)
						.symbol(componentStorage))
				.with(PartialRelationTranslator.of(dependsOn)
						.symbol(dependsOnStorage)
						.may(Query.of("mayDependsOn", (builder, p1, p2) -> builder.clause(
								may(component.call(p1)),
								may(component.call(p2)),
								not(must(EQUALS_SYMBOL.call(p1, p2))),
								not(new ForbiddenView(dependsOnStorage).call(p1, p2))
						))))
				.with(DemoDiscreteEventTranslator.of(operational)
						.query(Query.of("operational_B4", (builder, p1) -> builder.clause(
								must(component.call(p1)),
								must(dependsOn.call(p1, Variable.of()))
								)
						)))
				.build();

		var modelSeed = ModelSeed.builder(4)
				.seed(EXISTS_SYMBOL, builder -> builder
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE)
						.put(Tuple.of(3), TruthValue.TRUE))
				.seed(EQUALS_SYMBOL, builder -> builder
						.put(Tuple.of(0, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 1), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(3, 3), TruthValue.TRUE))
				.seed(component, builder -> builder
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.UNKNOWN))
				.seed(dependsOn, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.FALSE))
				.build();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, dependsOn);
		var friendRefiner = reasoningAdapter.getRefiner(dependsOn);

		for(AnyQuery query : model.getAdapter(ModelQueryAdapter.class).getStoreAdapter().getQueries()){
			System.out.println(query.name());
			try {
				if(model.getAdapter(ModelQueryAdapter.class).getResultSet(query) instanceof ResultSet<?> result){
					var cursor = result.getAll();
					while (cursor.move()){
						System.out.println("\t"+cursor.getKey()+" --> "+ cursor.getValue());
					}
				}
			} catch (IllegalArgumentException e){
				System.out.println("\t Error: " + e.getMessage());
			}

		}
		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
		assertThat(friendInterpretation.get(Tuple.of(3, 0)), is(TruthValue.FALSE));

		assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.FALSE), is(true));
		assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
		var splitResult = reasoningAdapter.split(1);
		assertThat(splitResult, Matchers.not(nullValue()));
		var newPerson = splitResult.get(0);
		queryAdapter.flushChanges();

		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.ERROR));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.TRUE));
		assertThat(friendInterpretation.get(Tuple.of(0, newPerson)), is(TruthValue.ERROR));
		assertThat(friendInterpretation.get(Tuple.of(newPerson, 0)), is(TruthValue.TRUE));
	}
}
