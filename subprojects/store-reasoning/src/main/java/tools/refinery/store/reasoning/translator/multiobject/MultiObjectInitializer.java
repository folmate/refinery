/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;

class MultiObjectInitializer implements PartialModelInitializer {
	private final Symbol<CardinalityInterval> countSymbol;

	public MultiObjectInitializer(Symbol<CardinalityInterval> countSymbol) {
		this.countSymbol = countSymbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var intervals = new CardinalityInterval[modelSeed.getNodeCount()];
		Arrays.fill(intervals, CardinalityIntervals.SET);
		initializeExists(intervals, modelSeed);
		initializeEquals(intervals, modelSeed);
		var countInterpretation = model.getInterpretation(countSymbol);
		for (int i = 0; i < intervals.length; i++) {
			var interval = intervals[i];
			if (interval.isEmpty()) {
				throw new IllegalArgumentException("Inconsistent existence or equality for node " + i);
			}
			countInterpretation.put(Tuple.of(i), intervals[i]);
		}
	}

	private void initializeExists(CardinalityInterval[] intervals, ModelSeed modelSeed) {
		var cursor = modelSeed.getCursor(ReasoningAdapter.EXISTS_SYMBOL, TruthValue.UNKNOWN);
		while (cursor.move()) {
			int i = cursor.getKey().get(0);
			checkNodeId(intervals, i);
			switch (cursor.getValue()) {
			case TRUE -> intervals[i] = intervals[i].meet(CardinalityIntervals.SOME);
			case FALSE -> intervals[i] = intervals[i].meet(CardinalityIntervals.NONE);
			case ERROR -> throw new IllegalArgumentException("Inconsistent existence for node " + i);
			default -> throw new IllegalArgumentException("Invalid existence truth value %s for node %d"
					.formatted(cursor.getValue(), i));
			}
		}
	}

	private void initializeEquals(CardinalityInterval[] intervals, ModelSeed modelSeed) {
		var seed = modelSeed.getSeed(ReasoningAdapter.EQUALS_SYMBOL);
		var cursor = seed.getCursor(TruthValue.FALSE, modelSeed.getNodeCount());
		while (cursor.move()) {
			var key = cursor.getKey();
			int i = key.get(0);
			int otherIndex = key.get(1);
			if (i != otherIndex) {
				throw new IllegalArgumentException("Off-diagonal equivalence (%d, %d) is not permitted"
						.formatted(i, otherIndex));
			}
			checkNodeId(intervals, i);
			switch (cursor.getValue()) {
			case TRUE -> intervals[i] = intervals[i].meet(CardinalityIntervals.LONE);
			case UNKNOWN -> {
				// Nothing do to, {@code intervals} is initialized with unknown equality.
			}
			case ERROR -> throw new IllegalArgumentException("Inconsistent equality for node " + i);
			default -> throw new IllegalArgumentException("Invalid equality truth value %s for node %d"
					.formatted(cursor.getValue(), i));
			}
		}
		for (int i = 0; i < intervals.length; i++) {
			if (seed.get(Tuple.of(i, i)) == TruthValue.FALSE) {
				throw new IllegalArgumentException("Inconsistent equality for node " + i);
			}
		}
	}

	private void checkNodeId(CardinalityInterval[] intervals, int nodeId) {
		if (nodeId < 0 || nodeId >= intervals.length) {
			throw new IllegalArgumentException("Expected node id %d to be lower than model size %d"
					.formatted(nodeId, intervals.length));
		}
	}
}
