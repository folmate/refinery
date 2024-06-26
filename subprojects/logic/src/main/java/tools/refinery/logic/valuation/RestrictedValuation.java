/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.valuation;

import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.DataVariable;

import java.util.Set;

public record RestrictedValuation(Valuation valuation, Set<AnyDataVariable> allowedVariables) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		if (!allowedVariables.contains(variable)) {
			throw new IllegalArgumentException("Variable %s is not in scope".formatted(variable));
		}
		return valuation.getValue(variable);
	}
}
