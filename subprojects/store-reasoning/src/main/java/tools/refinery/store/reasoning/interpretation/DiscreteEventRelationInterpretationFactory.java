package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.event.EventValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.representation.event.DiscreteEventValue;
import tools.refinery.store.tuple.Tuple;

public class DiscreteEventRelationInterpretationFactory implements PartialInterpretation.Factory<EventValue,
		EventValue> {
	private final FunctionalQuery<DiscreteEventValue> event;

	public DiscreteEventRelationInterpretationFactory(FunctionalQuery<DiscreteEventValue> event){
		this.event = event;
	}
	@Override
	public PartialInterpretation<EventValue, EventValue> create(ReasoningAdapter adapter,
																	 Concreteness concreteness,
																PartialSymbol<EventValue, EventValue> partialSymbol) {
		var queryEngine = adapter.getModel().getAdapter(ModelQueryAdapter.class);
		ResultSet<DiscreteEventValue> mayResultSet = queryEngine.getResultSet(event);

		return new DiscreteEventRelationInterpretationFactory.DiscreteEventInterpretation(adapter, concreteness,
				partialSymbol, mayResultSet);
	}


	private static class DiscreteEventInterpretation extends AbstractPartialInterpretation<EventValue,EventValue> {
		private final ResultSet<DiscreteEventValue> resultSet;

		protected DiscreteEventInterpretation(
				ReasoningAdapter adapter, Concreteness concreteness,
				PartialSymbol<EventValue, EventValue> partialSymbol,
				ResultSet<DiscreteEventValue> resultSet) {
			super(adapter, concreteness, partialSymbol);
			this.resultSet = resultSet;
		}

		@Override
		public DiscreteEventValue get(Tuple key) {
			return resultSet.get(key);
		}

		@Override
		public Cursor<Tuple, EventValue> getAll() {
			return new DiscreteEventRelationInterpretationFactory.DiscreteEventInterpretation.TmpEventCursor(resultSet.getAll());
		}

		private record TmpEventCursor(Cursor<Tuple, DiscreteEventValue> cursor) implements Cursor<Tuple,
				EventValue> {
			@Override
			public Tuple getKey() {
				return cursor.getKey();
			}

			@Override
			public DiscreteEventValue getValue() {
				return cursor.getValue();
			}

			@Override
			public boolean isTerminated() {
				return cursor.isTerminated();
			}

			@Override
			public boolean move() {
				return cursor.move();
			}
		}
	}
}
