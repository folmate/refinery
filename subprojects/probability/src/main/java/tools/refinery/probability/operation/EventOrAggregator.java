package tools.refinery.probability.operation;

import tools.refinery.logic.term.StatefulAggregate;
import tools.refinery.logic.term.StatefulAggregator;
import tools.refinery.probability.terms.EventHandle;

import java.util.ArrayList;
import java.util.List;

public class EventOrAggregator implements StatefulAggregator<EventHandle, EventHandle> {
	@Override
	public StatefulAggregate<EventHandle, EventHandle> createEmptyAggregate() {
		return new OrOperator();
	}

	@Override
	public Class<EventHandle> getResultType() {
		return EventHandle.class;
	}

	@Override
	public Class<EventHandle> getInputType() {
		return EventHandle.class;
	}

	static class OrOperator implements StatefulAggregate<EventHandle, EventHandle> {
		private final List<EventHandle> handles;

		public OrOperator() {
			this.handles = new ArrayList<>();
		}

		public OrOperator(List<EventHandle> numbers) {
			this.handles = new ArrayList<>(numbers);
			//this.handles.addAll(numbers);
		}

		@Override
		public void add(EventHandle value) {
			handles.add(value);
		}

		@Override
		public void remove(EventHandle value) {
			handles.remove(value);
		}

		@Override
		public EventHandle getResult() {
			double buff = 0;
			for (EventHandle handle : handles) {
				buff += handle.getValue();
			}
			return new EventHandle(buff);
		}

		@Override
		public boolean isEmpty() {
			return handles.isEmpty();
		}

		@Override
		public StatefulAggregate<EventHandle, EventHandle> deepCopy() {
			return new OrOperator(handles);
		}
	}
}
