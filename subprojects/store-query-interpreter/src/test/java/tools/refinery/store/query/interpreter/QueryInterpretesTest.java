package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.StatefulAggregate;
import tools.refinery.logic.term.StatefulAggregator;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public class QueryInterpretesTest {
	private static final Symbol<Boolean> node = Symbol.of("Nodes", 1);
	private static final Symbol<Integer> link = Symbol.of("Links", 2, Integer.class, null);
	private static final AnySymbolView nodeView = new KeyOnlyView<>(node);
	private static final FunctionView<Integer> linkView = new FunctionView<>(link);

	private final FunctionalQuery<Integer> query = Query.of(Integer.class, (builder, p1, p2, output) -> builder
			.clause(
					nodeView.call(p1),
					nodeView.call(p2),
					linkView.call(p1,p2,output)//.toLiteral(output)
					//out.o
					//output.assign(valuesView.aggregate(new InstrumentedAggregator(), p1, Variable.of()))
			));
	private final FunctionalQuery<Integer> query2 = Query.of(Integer.class, (builder, p1, output) -> builder
			.clause(
					query.call(p1,Variable.of()).toLiteral(output)
					//personView.call(p1),
					//output.assign(valuesView.aggregate(new InstrumentedAggregatorMax(), p1, Variable.of()))
			));
	private final Query<Integer> query3 = Query.of(Integer.class, (builder, p1, output) -> builder
			.clause(
					output.assign(query2.aggregate(new InstrumentedAggregatorMax(), p1)),
					query2.call(p1).toLiteral(Variable.of(Integer.class))
					//personView.call(p1),
					//output.assign(valuesView.aggregate(new InstrumentedAggregatorMax(), p1, Variable.of()))
			));

	@Test
	void batchTest() {
		var model = createModel();
		var nodeInterpretation = model.getInterpretation(node);
		var linkInterpretation = model.getInterpretation(link);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var resultSet = queryEngine.getResultSet(query);
		var resultSet2 = queryEngine.getResultSet(query2);
		var resultSet3 = queryEngine.getResultSet(query3);

		nodeInterpretation.put(Tuple.of(0), true);
		nodeInterpretation.put(Tuple.of(1), true);
		nodeInterpretation.put(Tuple.of(2), true);

		linkInterpretation.put(Tuple.of(0,1), 1);
		linkInterpretation.put(Tuple.of(0,2), 2);

		queryEngine.flushChanges();

		System.out.println("Out1");
		var matches = resultSet.getAll();
		while(matches.move()){
			System.out.println(matches.getKey() + " -> " + matches.getValue());
		}
		/*System.out.println("Out2");
		var matches2 = resultSet2.getAll();
		while(matches2.move()){
			System.out.println(matches2.getKey() + " -> " + matches2.getValue());
		}*/
		System.out.println("Out3");
		var matches3 = resultSet3.getAll();
		while(matches3.move()){
			System.out.println(matches3.getKey() + " -> " + matches3.getValue());
		}

	}
	private Model createModel() {
		var store = ModelStore.builder()
				.symbols(node, link)
				.with(QueryInterpreterAdapter.builder()
						.queries(query, query2, query3))
				.build();
		return store.createEmptyModel();
	}
	class InstrumentedAggregatorMax implements StatefulAggregator<Integer, Integer> {
		@Override
		public Class<Integer> getResultType() {
			return Integer.class;
		}

		@Override
		public Class<Integer> getInputType() {
			return Integer.class;
		}

		@Override
		public StatefulAggregate<Integer, Integer> createEmptyAggregate() {
			return new InstrumentedAggregate();
		}
	}
	class InstrumentedAggregate implements StatefulAggregate<Integer, Integer> {
		private final List<Integer> numbers;

		public InstrumentedAggregate() {
			this.numbers = new ArrayList<>();
		}
		public InstrumentedAggregate(List<Integer> numbers) {
			this.numbers = new ArrayList<>();
			this.numbers.addAll(numbers);
		}

		@Override
		public void add(Integer value) {
			numbers.add(value);
		}

		@Override
		public void remove(Integer value) {
			numbers.remove(value);
		}

		@Override
		public Integer getResult() {
			int buff = 0;
			for(int i : numbers){
				buff += i;
			}
			return buff;
		}

		@Override
		public boolean isEmpty() {
			return numbers.isEmpty();
		}

		@Override
		public StatefulAggregate<Integer, Integer> deepCopy() {
			return new InstrumentedAggregate(numbers);
		}
	}
}
