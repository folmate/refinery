package tools.refinery.probability.terms;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;

import java.util.Map;

public class DiscreteEventHandle extends EventHandle {
	private final Map<String, Double> outcomes;
	public DiscreteEventHandle(Map<String, Double> outcomes) {
		super(-1);
		this.outcomes = outcomes;
	}
	public EventHandle select(boolean inverse, String... keys){
		double p = 0.0;
		for(String key : keys){
			p += outcomes.get(key);
		}
		return new EventHandle(p);
	}
	public static Term<DiscreteEventHandle> of(Map<String,Double> outcomes){
		return new ConstantTerm<DiscreteEventHandle>(DiscreteEventHandle.class, new DiscreteEventHandle(outcomes));
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
