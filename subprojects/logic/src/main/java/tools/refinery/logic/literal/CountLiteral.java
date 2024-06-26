/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.Variable;

import java.util.List;

public class CountLiteral extends AbstractCountLiteral<Integer> {
	public CountLiteral(DataVariable<Integer> resultVariable, Constraint target, List<Variable> arguments) {
		super(Integer.class, resultVariable, target, arguments);
	}

	@Override
	protected Integer zero() {
		return 0;
	}

	@Override
	protected Integer one() {
		return 1;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountLiteral(substitution.getTypeSafeSubstitute(getResultVariable()), getTarget(),
				substitutedArguments);
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new CountLiteral(getResultVariable(), newTarget, newArguments);
	}

	@Override
	protected String operatorName() {
		return "count";
	}
}
