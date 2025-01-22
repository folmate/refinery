package tools.refinery.store.reasoning.representation.event;

import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Map;

public class DiscreteEventRelation extends EventRelation<DiscreteEventHandle> {
	private final Map<String, Double> outcomes;
	public DiscreteEventRelation(String name, PartialRelation partialRelation, Map<String, Double> outcomes) {
		super(name, partialRelation);
		this.outcomes = outcomes;
	}
	public EventHandle select(String... keys){
		double p = 0.0;
		for(String key : keys){
			p += outcomes.get(key);
		}
		return new EventHandle(p);
	}
	public Map<String,Double> getOutcomes(){
		return outcomes;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(super.toString());
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
