/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.valuation;

import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;

import java.util.Map;

record MapBasedValuation(Map<AnyDataVariable, Object> values) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		if (!values.containsKey(variable)) {
			throwNoValueException(variable);
		}
		var value = values.get(variable);
		return variable.getType().cast(value);
	}

	@Override
	public Integer getNodeId(NodeVariable nodeVariable) {
		return throwNoValueException(nodeVariable);
	}

	private <T> T throwNoValueException(Variable variable) {
		throw new IllegalArgumentException("No value for variable %s".formatted(variable));
	}
}
