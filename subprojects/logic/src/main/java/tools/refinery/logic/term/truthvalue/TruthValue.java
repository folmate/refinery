/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;

public enum TruthValue implements AbstractValue<TruthValue, Boolean> {
	TRUE("true"),

	FALSE("false"),

	UNKNOWN("unknown"),

	ERROR("error");

	private final String name;

	TruthValue(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static TruthValue toTruthValue(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	@Nullable
	public Boolean getArbitrary() {
		return switch (this) {
			case TRUE -> true;
			case FALSE, UNKNOWN -> false;
			case ERROR -> null;
		};
	}

	@Override
	public boolean isError() {
		return this == ERROR;
	}

	public boolean isConsistent() {
		return !isError();
	}

	public boolean isComplete() {
		return this != UNKNOWN;
	}

	@Override
	@Nullable
	public Boolean getConcrete() {
		return switch (this) {
			case TRUE -> true;
			case FALSE -> false;
			default -> null;
		};
	}

	@Override
	public boolean isConcrete() {
		return this == TRUE || this == FALSE;
	}

	public boolean must() {
		return this == TRUE || this == ERROR;
	}

	public boolean may() {
		return this == TRUE || this == UNKNOWN;
	}

	public TruthValue not() {
		return switch (this) {
			case TRUE -> FALSE;
			case FALSE -> TRUE;
			default -> this;
		};
	}

	@Override
	public TruthValue join(TruthValue other) {
		return switch (this) {
			case TRUE -> other == ERROR || other == TRUE ? TRUE : UNKNOWN;
			case FALSE -> other == ERROR || other == FALSE ? FALSE : UNKNOWN;
			case UNKNOWN -> UNKNOWN;
			case ERROR -> other;
		};
	}

	@Override
	public TruthValue meet(TruthValue other) {
		return switch (this) {
			case TRUE -> other == UNKNOWN || other == TRUE ? TRUE : ERROR;
			case FALSE -> other == UNKNOWN || other == FALSE ? FALSE : ERROR;
			case UNKNOWN -> other;
			case ERROR -> ERROR;
		};
	}
}
