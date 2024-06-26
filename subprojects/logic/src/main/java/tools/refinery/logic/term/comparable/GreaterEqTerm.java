/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.comparable;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Term;

public class GreaterEqTerm<T extends Comparable<T>> extends ComparisonTerm<T> {
	public GreaterEqTerm(Class<T> argumentType, Term<T> left, Term<T> right) {
		super(argumentType, left, right);
	}

	@Override
	protected Boolean doEvaluate(T leftValue, T rightValue) {
		return leftValue.compareTo(rightValue) >= 0;
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<T> substitutedLeft, Term<T> substitutedRight) {
		return new GreaterEqTerm<>(getArgumentType(), substitutedLeft, substitutedRight);
	}

	@Override
	public String toString() {
		return "(%s >= %s)".formatted(getLeft(), getRight());
	}
}
