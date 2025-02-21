package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.event.EventValue;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.DiscreteEventRelationV2;
import tools.refinery.store.reasoning.representation.event.DiscreteEventValue;

import java.util.List;
import java.util.Set;

public class DiscreteEventRelationRewriter implements PartialRelationRewriter{
	private final FunctionalQuery<DiscreteEventValue> event;

	public DiscreteEventRelationRewriter(FunctionalQuery<DiscreteEventValue> event){
		this.event = event;
	}
	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal, Modality modality, Concreteness concreteness) {
		//FIXME ignore modality and concreteness for now. (And likely forever.)
		return List.of(literal.withTarget(event.getDnf()));
	}
}
