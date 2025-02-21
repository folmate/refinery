package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.Parameter;
import tools.refinery.logic.term.event.EventValue;
import tools.refinery.logic.term.event.EventValueDomain;

import java.util.Arrays;
import java.util.List;

//FIXME Check if this should be a record
//FIXME Check if this should be [a | an Abstract] FunctionView (but at least SymbolView)
public abstract sealed class EventRelationV2 implements PartialSymbol<EventValue, EventValue>, Constraint permits DiscreteEventRelationV2 {
	private final String name;
	private final int arity;

	public EventRelationV2(String name, int arity){
		this.name = name;
		this.arity = arity;
	}
	@Override
	public List<Parameter> getParameters() {
		var parameters = new Parameter[arity];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		return List.of(parameters);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int arity() {
		return arity;
	}

	@Override
	public AbstractDomain<EventValue, EventValue> abstractDomain() {
		return EventValueDomain.INSTANCE;
	}

	@Override
	public EventValue defaultValue() {
		return null;
	}
	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
