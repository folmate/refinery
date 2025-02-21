package tools.refinery.logic.term.event;

import tools.refinery.logic.AbstractDomain;

public class EventValueDomain<T> implements AbstractDomain<EventValue, EventValue> {
	public static final EventValueDomain<?> INSTANCE = new EventValueDomain<>();
	private final EventValue error = EventValue.of(Double.NaN);
	private final EventValue unknown = EventValue.of(-1);
	private EventValueDomain(){
	}

	@Override
	public Class<EventValue> abstractType() {
		return EventValue.class;
	}

	@Override
	public Class<EventValue> concreteType() {
		return EventValue.class;
	}

	@Override
	public EventValue unknown() {
		return unknown;
	}

	@Override
	public EventValue error() {
		return error;
	}

	@Override
	public EventValue toAbstract(EventValue concreteValue) {
		return concreteValue;
	}
}
