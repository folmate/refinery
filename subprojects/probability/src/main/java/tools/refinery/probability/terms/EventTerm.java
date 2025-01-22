package tools.refinery.probability.terms;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.*;
import tools.refinery.logic.valuation.Valuation;

import java.util.Set;
import java.util.stream.Stream;

public class EventTerm extends AbstractTerm<EventHandle> {
	private final int id;
	private final EventComputationMode mode;
	private final DataVariable<EventHandle>[] args;

	@Deprecated
	protected EventTerm(int id) {
		super(EventHandle.class);
		this.id = id;
		args = null;
		mode = EventComputationMode.DISCRETE;
	}
	public EventTerm(EventComputationMode mode, DataVariable<EventHandle>... args){
		super(EventHandle.class);
		this.id = 0;
		this.mode = mode;
		this.args = args;

	}
	@Override
	public EventHandle evaluate(Valuation valuation) {
		var values = Stream.of(args).map(valuation::getValue).toArray();
		double buff = 1.0;
		for(DataVariable<EventHandle> variable :  args){
			buff *= valuation.getValue(variable).getValue();
		}
		return new EventHandle(buff);
	}

	@Override
	public Term<EventHandle> substitute(Substitution substitution) {
		return null;
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of();
	}
	public static EventTerm AND(DataVariable<EventHandle>... vars){
		return new EventTerm(EventComputationMode.AND, vars);
	}
}
