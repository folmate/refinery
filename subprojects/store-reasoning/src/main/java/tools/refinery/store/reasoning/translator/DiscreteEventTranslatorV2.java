package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.probability.terms.DiscreteEventHandle;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.interpretation.DiscreteEventRelationInterpretationFactory;
import tools.refinery.store.reasoning.interpretation.DiscreteEventRelationRewriter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.DiscreteEventRelationV2;
import tools.refinery.store.reasoning.representation.event.DiscreteEventValue;
import tools.refinery.store.reasoning.representation.event.EventRelation;

import static tools.refinery.store.reasoning.literal.PartialLiterals.must;


public final class DiscreteEventTranslatorV2 implements AnyPartialSymbolTranslator {
	private boolean configured = false;
	private DiscreteEventRelationV2 event;

	//TODO create relation rewriter to contain the queries,
	private DiscreteEventRelationRewriter rewriter;
	private DiscreteEventRelationInterpretationFactory interpreter;

	private RelationalQuery selector;
	private FunctionalQuery<DiscreteEventValue> query;

	public DiscreteEventTranslatorV2 rewriter(DiscreteEventRelationRewriter rewriter){
		checkNotConfigured();
		this.rewriter = rewriter;
		return this;
	}
	public DiscreteEventRelationRewriter getRewriter(){
		return rewriter;
	}

	public DiscreteEventTranslatorV2(DiscreteEventRelationV2 event) {
		this.event = event;
	}

	@Override
	public AnyPartialSymbol getPartialSymbol() {
		return event;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		checkNotConfigured();
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		var mustSelector = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, selector);

		/*
		 * Build event query
		 */
		var parameters = createParameters();
		var output = Variable.of(DiscreteEventValue.class);

		var outcomes = event.getOutcomes();
		var query = Query.builder(event.name())
				.parameters(parameters)
				.output(output)
				.clause(
						selector.call(parameters),
						output.assign(DiscreteEventValue.of(outcomes))
				)
				.build();
		/*
		 * Lift queries
		 */

		/*
		 * Create rewriter
		 * Create interpreter
		 */
		if(rewriter==null){
			rewriter = new DiscreteEventRelationRewriter(query);
		}
		if(interpreter==null){
			interpreter = new DiscreteEventRelationInterpretationFactory(query);
		}
		configured = true;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ReasoningBuilder.class).partialSymbol(this);
	}
	public static DiscreteEventTranslatorV2 of(DiscreteEventRelationV2 event){
		return new DiscreteEventTranslatorV2(event);
	}
	protected void checkNotConfigured() {
		if (configured) {
			throw new IllegalStateException("Partial symbol was already configured");
		}
	}
	public NodeVariable[] createParameters() {
		var parameters = new NodeVariable[event.arity()];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = Variable.of();
		}
		return parameters;
	}
}
