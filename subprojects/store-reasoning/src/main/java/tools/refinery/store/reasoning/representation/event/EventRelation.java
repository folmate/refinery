package tools.refinery.store.reasoning.representation.event;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.Parameter;
import tools.refinery.probability.terms.EventHandle;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.List;
import java.util.function.Function;

@Deprecated
public class EventRelation<T extends EventHandle> {
	public final String name;
	public final PartialRelation partialRelation;
	private FunctionalQuery<T> query;

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

	public void query(FunctionalQuery<T> query) {
		if(this.query!=null){
			throw new IllegalStateException("Query is already set.");
		}
		this.query = query;
	}

	public FunctionalQuery<T> query() {
		return query;
	}

	public List<String> parameterNames(Function<Parameter,String> mapper){
		return partialRelation.getParameters().stream().map(mapper).toList();
	}
}
