package tools.refinery.logic.term.event;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;

public class EventValue implements AbstractValue<EventValue, EventValue> {
	private double probability;
	protected EventValue(double probability){
		this.probability = probability;
	}
	@Override
	public @Nullable EventValue getConcrete() {
		return this;
	}

	@Override
	public @Nullable EventValue getArbitrary() {
		return this;
	}

	@Override
	public EventValue join(EventValue other) {
		return null;
	}

	@Override
	public EventValue meet(EventValue other) {
		return null;
	}

	public double getProbability(){
		return probability;
	}


	/*public static class EventHandleV2<H>{
		private H handle;
		public EventHandleV2(H handle){
			this.handle = handle;
		}
		public H getHandle(){
			return handle;
		}
		@Override
		public String toString(){
			return handle.toString();
		}
	}*/

	public static EventValue of(double probability){
		return new EventValue(probability);
	}
}
