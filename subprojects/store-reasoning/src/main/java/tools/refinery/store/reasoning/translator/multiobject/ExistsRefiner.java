/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

public class ExistsRefiner extends AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValue, Boolean> {
	private final Interpretation<CardinalityInterval> countInterpretation;

	private ExistsRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
						   Symbol<CardinalityInterval> countSymbol) {
		super(adapter, partialSymbol);
		countInterpretation = adapter.getModel().getInterpretation(countSymbol);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var currentCount = countInterpretation.get(key);
		if (currentCount == null) {
			return false;
		}
		CardinalityInterval newCount;
		switch (value) {
		case UNKNOWN -> {
			return true;
		}
		case TRUE -> newCount = currentCount.meet(CardinalityIntervals.SOME);
		case FALSE -> {
			if (concretizationInProgress()) {
				return false;
			}
			newCount = currentCount.meet(CardinalityIntervals.NONE);
		}
		case ERROR -> {
			if (concretizationInProgress()) {
				return false;
			}
			newCount = currentCount.meet(CardinalityIntervals.ERROR);
		}
		default -> throw new IllegalArgumentException("Unknown TruthValue: " + value);
		}
		countInterpretation.put(key, newCount);
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<CardinalityInterval> countSymbol) {
		return (adapter, partialSymbol) -> new ExistsRefiner(adapter, partialSymbol, countSymbol);
	}
}
