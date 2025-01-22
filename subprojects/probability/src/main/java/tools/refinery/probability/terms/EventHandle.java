package tools.refinery.probability.terms;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;

public class EventHandle {
	private double handle;
	public EventHandle(double id){
		this.handle = id;
	}
	public double getValue(){
		return handle;
	}

	public static Term<EventHandle> of(double id){
		return new ConstantTerm<EventHandle>(EventHandle.class, new EventHandle(id));
	}
	@Override
	public String toString(){
		return "%f".formatted(handle);
	}

}
