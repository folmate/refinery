package tools.refinery.store.reasoning.representation;

import java.util.Map;

public final class DiscreteEventRelationV2 extends EventRelationV2 {
	private final Map<String, Double> outcomes;

	public DiscreteEventRelationV2(String name, int arity, Map<String, Double> outcomes) {
		super(name, arity);
		this.outcomes = outcomes;
	}
	public Map<String, Double> getOutcomes(){
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
