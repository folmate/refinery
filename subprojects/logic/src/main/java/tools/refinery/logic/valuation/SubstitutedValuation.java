/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.valuation;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;

public record SubstitutedValuation(Valuation originalValuation, Substitution substitution) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		return originalValuation.getValue(substitution.getTypeSafeSubstitute(variable));
	}

	@Override
	public Integer getNodeId(NodeVariable nodeVariable) {
		return originalValuation.getNodeId(substitution.getTypeSafeSubstitute(nodeVariable));
	}
}
