package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.Parameter;
import tools.refinery.probability.terms.EventHandle;

import java.util.List;
import java.util.function.Function;

public class EventRelation {
	public final String name;
	public final PartialRelation partialRelation;
	private FunctionalQuery<EventHandle> query;

	public EventRelation(String name, PartialRelation partialRelation){
		this.name = name;
		this.partialRelation = partialRelation;
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table look-ups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, partialRelation.arity());
	}
	public int arity(){
		return partialRelation.arity();
	}

	public void query(FunctionalQuery<EventHandle> query) {
		if(this.query!=null){
			throw new IllegalStateException("Query is already set.");
		}
		this.query = query;
	}

	public FunctionalQuery<EventHandle> query() {
		return query;
	}
	public List<String> parameterNames(Function<Parameter,String> mapper){
		return partialRelation.getParameters().stream().map(mapper).toList();
	}
}
