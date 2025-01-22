package tools.refinery.probability.terms;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.AbstractTerm;
import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.valuation.Valuation;

import java.util.Set;

public class SelectEventTerm extends AbstractTerm<EventHandle> {
	private final DataVariable<DiscreteEventHandle> source;
	private final String[] selected;
	private final boolean inverse;

	public SelectEventTerm(DataVariable<DiscreteEventHandle> variable, boolean inverse, String... decisions) {
		super(EventHandle.class);
		this.source = variable;
		this.inverse = inverse;
		this.selected = decisions;
	}

	public SelectEventTerm(DataVariable<DiscreteEventHandle> variable, String... decisions) {
		this(variable, false, decisions);
	}
	@Override
	public EventHandle evaluate(Valuation valuation) {
		return valuation.getValue(source).select(inverse, selected);
	}

	@Override
	public Term<EventHandle> substitute(Substitution substitution) {
		return null; //FIXME
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of(source);
	}
}
