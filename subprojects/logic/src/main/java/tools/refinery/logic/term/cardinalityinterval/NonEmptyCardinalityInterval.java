/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;

public record NonEmptyCardinalityInterval(int lowerBound, UpperCardinality upperBound) implements CardinalityInterval {
	public NonEmptyCardinalityInterval {
		if (lowerBound < 0) {
			throw new IllegalArgumentException("lowerBound must not be negative");
		}
		if (upperBound.compareToInt(lowerBound) < 0) {
			throw new IllegalArgumentException("lowerBound must not be larger than upperBound");
		}
	}

	@Override
	@Nullable
	public Integer getConcrete() {
        return isConcrete() ? lowerBound : null;
    }

	@Override
	public boolean isConcrete() {
		return upperBound instanceof FiniteUpperCardinality(var finiteUpperBound) && finiteUpperBound == lowerBound;
	}

	@Override
	@NotNull
	public Integer getArbitrary() {
		return lowerBound;
	}

	@Override
	public boolean isError() {
		return false;
	}

	@Override
	public boolean isRefinementOf(CardinalityInterval other) {
		if (!(other instanceof NonEmptyCardinalityInterval(var otherLowerBound, var otherUpperBound))) {
			return false;
		}
		return lowerBound >= otherLowerBound && upperBound.compareTo(otherUpperBound) <= 0;
	}

	@Override
	public CardinalityInterval min(CardinalityInterval other) {
		return lift(other, Math::min, UpperCardinality::min);
	}

	@Override
	public CardinalityInterval max(CardinalityInterval other) {
		return lift(other, Math::max, UpperCardinality::max);
	}

	@Override
	public CardinalityInterval add(CardinalityInterval other) {
		return lift(other, Integer::sum, UpperCardinality::add);
	}

	@Override
	public CardinalityInterval multiply(CardinalityInterval other) {
		return lift(other, (a, b) -> a * b, UpperCardinality::multiply);
	}

	@Override
	public CardinalityInterval meet(CardinalityInterval other) {
		return lift(other, Math::max, UpperCardinality::min);
	}

	@Override
	public CardinalityInterval join(CardinalityInterval other) {
		return lift(other, Math::min, UpperCardinality::max, this);
	}

	@Override
	public CardinalityInterval take(int count) {
		int newLowerBound = Math.max(lowerBound - count, 0);
		var newUpperBound = upperBound.take(count);
		if (newUpperBound == null) {
			return CardinalityIntervals.ERROR;
		}
		return CardinalityIntervals.between(newLowerBound, newUpperBound);
	}

	private CardinalityInterval lift(CardinalityInterval other, IntBinaryOperator lowerOperator,
									 BinaryOperator<UpperCardinality> upperOperator,
									 CardinalityInterval whenEmpty) {
		if (other instanceof NonEmptyCardinalityInterval(var otherLowerBound, var otherUpperBound)) {
			return CardinalityIntervals.between(lowerOperator.applyAsInt(lowerBound, otherLowerBound),
					upperOperator.apply(upperBound, otherUpperBound));
		}
		if (other instanceof EmptyCardinalityInterval) {
			return whenEmpty;
		}
		throw new IllegalArgumentException("Unknown CardinalityInterval: " + other);
	}

	private CardinalityInterval lift(CardinalityInterval other, IntBinaryOperator lowerOperator,
									 BinaryOperator<UpperCardinality> upperOperator) {
		return lift(other, lowerOperator, upperOperator, CardinalityIntervals.ERROR);
	}

	@Override
	public String toString() {
		if (upperBound instanceof FiniteUpperCardinality(var finiteUpperBound) &&
				finiteUpperBound == lowerBound) {
			return "[%d]".formatted(lowerBound);
		}
		return "[%d..%s]".formatted(lowerBound, upperBound);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NonEmptyCardinalityInterval that = (NonEmptyCardinalityInterval) o;
		return lowerBound == that.lowerBound && Objects.equals(upperBound, that.upperBound);
	}

	@Override
	public int hashCode() {
		return lowerBound * 31 + upperBound.hashCode();
	}
}
