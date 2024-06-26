/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface PartialInterpretationRefiner<A extends AbstractValue<A, C>, C>
		extends AnyPartialInterpretationRefiner {
	@Override
	PartialSymbol<A, C> getPartialSymbol();

	boolean merge(Tuple key, A value);

	@FunctionalInterface
	interface Factory<A extends AbstractValue<A, C>, C> {
		PartialInterpretationRefiner<A, C> create(ReasoningAdapter adapter, PartialSymbol<A, C> partialSymbol);
	}
}
