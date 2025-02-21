package tools.refinery.store.reasoning.representation.event;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.event.EventValue;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.store.reasoning.representation.DiscreteEventRelationV2;

import java.util.Map;

public class DiscreteEventValue extends EventValue {
	private final Map<String, Double> outcomes;
	public DiscreteEventValue(Map<String, Double> outcomes){
		super(1);
		this.outcomes = outcomes;
	}

	@Override
	public @Nullable DiscreteEventValue getConcrete() {
		return null;
	}

	@Override
	public @Nullable DiscreteEventValue getArbitrary() {
		return null;
	}

	public static Term<DiscreteEventValue> of(Map<String,Double> outcomes){
		return new ConstantTerm<DiscreteEventValue>(DiscreteEventValue.class, new DiscreteEventValue(outcomes));
	}

	@Override
	public String toString(){
		var builder = new StringBuilder();
		builder.append("{");
		boolean first = true;
		for(Map.Entry<String,Double> entry : outcomes.entrySet()){
			if(!first){
				builder.append(",");
			}
			first = false;
			builder.append(entry.getKey()).append("->").append(entry.getValue());
		}
		builder.append("}");
		return builder.toString();
	}
}
