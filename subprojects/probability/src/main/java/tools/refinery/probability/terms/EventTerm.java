package tools.refinery.probability.terms;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.AbstractTerm;
import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.valuation.Valuation;

import java.util.Set;

public class EventTerm extends AbstractTerm<EventTerm> {
	private final int id;

	protected EventTerm(int id) {
		super(EventTerm.class);
		this.id = id;
	}

	@Override
	public EventTerm evaluate(Valuation valuation) {
		return null;
	}

	@Override
	public Term<EventTerm> substitute(Substitution substitution) {
		return null;
	}

	@Override
	public Set<AnyDataVariable> getInputVariables() {
		return Set.of();
	}

}
