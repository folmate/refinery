/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.tuple.*;

import java.util.Iterator;

final class MatcherUtils {
	private MatcherUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static tools.refinery.interpreter.matchers.tuple.Tuple toInterpreterTuple(Tuple refineryTuple) {
		return switch (refineryTuple) {
			case Tuple0 ignored -> Tuples.staticArityFlatTupleOf();
			case Tuple1 tuple1 -> Tuples.staticArityFlatTupleOf(tuple1);
			case Tuple2(int value0, int value1) -> Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1));
			case Tuple3(int value0, int value1, int value2) ->
					Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1), Tuple.of(value2));
			case Tuple4(int value0, int value1, int value2, int value3) ->
					Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1), Tuple.of(value2),
							Tuple.of(value3));
			default -> {
				int arity = refineryTuple.getSize();
				var values = new Object[arity];
				for (int i = 0; i < arity; i++) {
					values[i] = Tuple.of(refineryTuple.get(i));
				}
				yield Tuples.flatTupleOf(values);
			}
		};
	}

	public static Tuple toRefineryTuple(ITuple viatraTuple) {
		int arity = viatraTuple.getSize();
		if (arity == 1) {
			return getWrapper(viatraTuple, 0);
		}
		return prefixToRefineryTuple(viatraTuple, viatraTuple.getSize());
	}

	public static Tuple keyToRefineryTuple(ITuple viatraTuple) {
		return prefixToRefineryTuple(viatraTuple, viatraTuple.getSize() - 1);
	}

	private static Tuple prefixToRefineryTuple(ITuple viatraTuple, int targetArity) {
		if (targetArity < 0) {
			throw new IllegalArgumentException("Requested negative prefix %d of %s"
					.formatted(targetArity, viatraTuple));
		}
		return switch (targetArity) {
			case 0 -> Tuple.of();
			case 1 -> Tuple.of(unwrap(viatraTuple, 0));
			case 2 -> Tuple.of(unwrap(viatraTuple, 0), unwrap(viatraTuple, 1));
			case 3 -> Tuple.of(unwrap(viatraTuple, 0), unwrap(viatraTuple, 1), unwrap(viatraTuple, 2));
			case 4 -> Tuple.of(unwrap(viatraTuple, 0), unwrap(viatraTuple, 1), unwrap(viatraTuple, 2),
					unwrap(viatraTuple, 3));
			default -> {
				var entries = new int[targetArity];
				for (int i = 0; i < targetArity; i++) {
					entries[i] = unwrap(viatraTuple, i);
				}
				yield Tuple.of(entries);
			}
		};
	}

	private static Tuple1 getWrapper(ITuple viatraTuple, int index) {
		if (!((viatraTuple.get(index)) instanceof Tuple1 wrappedObjectId)) {
			throw new IllegalArgumentException("Element %d of tuple %s is not an object id"
					.formatted(index, viatraTuple));
		}
		return wrappedObjectId;
	}

	private static int unwrap(ITuple viatraTuple, int index) {
		return getWrapper(viatraTuple, index).value0();
	}

	public static <T> T getValue(ITuple match) {
		// This is only safe if we know for sure that match came from a functional query of type {@code T}.
		@SuppressWarnings("unchecked")
		var result = (T) match.get(match.getSize() - 1);
		return result;
	}

	public static <T> T getSingleValue(@Nullable Iterable<? extends ITuple> viatraTuples) {
		if (viatraTuples == null) {
			return null;
		}
		return getSingleValue(viatraTuples.iterator());
	}

	public static <T> T getSingleValue(Iterator<? extends ITuple> iterator) {
		if (!iterator.hasNext()) {
			return null;
		}
		var match = iterator.next();
		var result = MatcherUtils.<T>getValue(match);
		if (iterator.hasNext()) {
			var input = keyToRefineryTuple(match);
			throw new IllegalStateException("Query is not functional for input tuple: " + input);
		}
		return result;
	}
}
