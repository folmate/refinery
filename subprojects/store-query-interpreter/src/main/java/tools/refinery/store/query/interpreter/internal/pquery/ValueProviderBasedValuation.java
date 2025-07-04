/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.matchers.psystem.IValueProvider;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.valuation.Valuation;
import tools.refinery.store.tuple.Tuple1;

public record ValueProviderBasedValuation(IValueProvider valueProvider) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		@SuppressWarnings("unchecked")
		var value = (T) valueProvider.getValue(variable.getUniqueName());
		return value;
	}

	public Integer getNodeId(NodeVariable nodeVariable) {
		var value = (Tuple1) valueProvider.getValue(nodeVariable.getUniqueName());
		return value.value0();
	}
}
