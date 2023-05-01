/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.term.Variable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class VariableBinder {
	public static final VariableBinder EMPTY = new VariableBinder(Map.of());

	private final Map<Variable, VariableDirection> directionMap;

	VariableBinder(Map<Variable, VariableDirection> directionMap) {
		this.directionMap = directionMap;
	}

	public VariableDirection getDirection(Variable variable) {
		var direction = directionMap.get(variable);
		if (direction == null) {
			throw new IllegalArgumentException("No such variable " + variable);
		}
		return direction;
	}

	public VariableDirection getDirection(Variable variable, Set<? extends Variable> positiveVariables) {
		var direction = getDirection(variable);
		return disambiguateDirection(direction, variable, positiveVariables);
	}

	public Stream<Variable> getVariablesWithDirection(VariableDirection direction) {
		return directionMap.entrySet().stream()
				.filter(pair -> pair.getValue() == direction)
				.map(Map.Entry::getKey);
	}

	public Stream<Variable> getVariablesWithDirection(VariableDirection direction,
													  Set<? extends Variable> positiveVariables) {
		if (direction == VariableDirection.NEGATIVE) {
			throw new IllegalArgumentException("Use #getVariablesWithDirection(VariableDirection) if disambiguation " +
					"of VariableDirection#NEGATIVE variables according to the containing DnfClose is not desired");
		}
		return directionMap.entrySet().stream()
				.filter(pair -> disambiguateDirection(pair.getValue(), pair.getKey(), positiveVariables) == direction)
				.map(Map.Entry::getKey);
	}

	private VariableDirection disambiguateDirection(VariableDirection direction, Variable variable,
													Set<? extends Variable> positiveVariables) {
		if (direction != VariableDirection.NEGATIVE) {
			return direction;
		}
		return positiveVariables.contains(variable) ? VariableDirection.IN : VariableDirection.CLOSURE;
	}

	public static VariableBinderBuilder builder() {
		return new VariableBinderBuilder();
	}
}
