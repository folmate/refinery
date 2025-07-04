/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import com.google.ortools.linearsolver.MPConstraint;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.tuple.Tuple;

import java.util.Collection;

abstract class TypeScopePropagator {
	private final BoundScopePropagator adapter;
	private final ResultSet<Boolean> allNodes;
	private final ResultSet<Boolean> multiNodes;
	private final CriterionCalculator acceptCalculator;
	private final PartialRelation type;
	protected final MPConstraint constraint;
	private String unsatisfiableMessage;
	private String notSatisfiedMessage;

	protected TypeScopePropagator(BoundScopePropagator adapter, RelationalQuery allQuery,
								  RelationalQuery multiQuery, Criterion acceptCriterion, PartialRelation type) {
		this.adapter = adapter;
		this.type = type;
		var queryEngine = adapter.getQueryEngine();
		this.acceptCalculator = acceptCriterion.createCalculator(queryEngine.getModel());
		allNodes = queryEngine.getResultSet(allQuery);
		multiNodes = queryEngine.getResultSet(multiQuery);
		constraint = adapter.makeConstraint();
		constraint.setBounds(0, Double.POSITIVE_INFINITY);
		var cursor = multiNodes.getAll();
		while (cursor.move()) {
			var variable = adapter.getVariable(cursor.getKey().get(0));
			constraint.setCoefficient(variable, 1);
		}
		allNodes.addListener(this::allChanged);
		multiNodes.addListener(this::multiChanged);
	}

	protected abstract void doUpdateBounds();

	public boolean updateBounds() {
		doUpdateBounds();
		return constraint.lb() <= constraint.ub();
	}

	public abstract String getName();

	public String getUnsatisfiableMessage() {
		if (unsatisfiableMessage == null) {
			unsatisfiableMessage = "Unsatisfiable %s.".formatted(getName());
		}
		return unsatisfiableMessage;
	}

	public String getNotSatisfiedMessage() {
		if (notSatisfiedMessage == null) {
			notSatisfiedMessage = "The %s was not satisfied.".formatted(getName());
		}
		return notSatisfiedMessage;
	}

	public PartialRelation getType() {
		return type;
	}

	public boolean checkConcretization() {
		return acceptCalculator.isSatisfied();
	}

	protected int getSingleCount() {
		return allNodes.size() - multiNodes.size();
	}

	private void allChanged(Tuple ignoredKey, Boolean ignoredOldValue, Boolean ignoredNewValue) {
		adapter.markAsChanged();
	}

	private void multiChanged(Tuple key, Boolean ignoredOldValue, Boolean newValue) {
		var variable = adapter.getVariable(key.get(0));
		constraint.setCoefficient(variable, Boolean.TRUE.equals(newValue) ? 1 : 0);
		adapter.markAsChanged();
	}

	public void delete() {
		constraint.delete();
	}

	abstract static class Factory {
		public abstract TypeScopePropagator createPropagator(BoundScopePropagator adapter);

		protected abstract Collection<AnyQuery> getQueries();

		protected abstract Criterion getAcceptCriterion();

		public void configure(ModelStoreBuilder storeBuilder) {
			storeBuilder.getAdapter(ModelQueryBuilder.class).queries(getQueries());
			getAcceptCriterion().configure(storeBuilder);
		}
	}
}
