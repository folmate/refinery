package tools.refinery.probability.terms;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;

public class EventHandle {
	private int handle;
	public EventHandle(int id){
		this.handle = id;
	}
	public int getValue(){
		return handle;
	}
	public static Term<EventHandle> of(int id){
		return new ConstantTerm<EventHandle>(EventHandle.class, new EventHandle(id));
	}
	@Override
	public String toString(){
		return "%d".formatted(handle);
	}
}
