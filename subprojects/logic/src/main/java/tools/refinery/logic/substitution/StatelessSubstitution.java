/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.substitution;

import tools.refinery.logic.term.Variable;

public enum StatelessSubstitution implements Substitution {
	FAILING {
		@Override
		public Variable getSubstitute(Variable variable) {
			throw new IllegalArgumentException("No substitute for " + variable);
		}
	},
	IDENTITY {
		@Override
		public Variable getSubstitute(Variable variable) {
			return variable;
		}
	}
}
