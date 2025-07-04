/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.AbstractQueryBuilder;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.*;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class BasePredicateTranslator implements ModelStoreConfiguration {
	private final PartialRelation predicate;
	private final List<PartialRelation> parameterTypes;
	private final Set<PartialRelation> supersets;
	private final TruthValue defaultValue;
	private final ConcretizationSettings concretizationSettings;
	private final Symbol<TruthValue> symbol;

	public BasePredicateTranslator(
			PartialRelation predicate, List<PartialRelation> parameterTypes, Set<PartialRelation> supersets,
			TruthValue defaultValue, ConcretizationSettings concretizationSettings) {
		this.predicate = predicate;
		this.parameterTypes = parameterTypes;
		this.supersets = supersets;
		this.defaultValue = defaultValue;
		this.concretizationSettings = concretizationSettings;
		symbol = Symbol.of(predicate.name(), predicate.arity(), TruthValue.class, defaultValue);
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		int arity = predicate.arity();
		if (arity != parameterTypes.size()) {
			throw new TranslationException(predicate,
					"Expected %d parameter type for base predicate %s, got %d instead"
							.formatted(arity, predicate, parameterTypes.size()));
		}
		if (defaultValue.must()) {
			throw new TranslationException(predicate, "Unsupported default value %s for base predicate %s"
					.formatted(defaultValue, predicate));
		}
		var translator = PartialRelationTranslator.of(predicate);
		translator.symbol(symbol);
		if (defaultValue.may()) {
			configureWithDefaultUnknown(translator);
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		var roundingMode = concretizationSettings.concretize() ? RoundingMode.PREFER_FALSE : RoundingMode.NONE;
		translator.refiner(PredicateRefiner.of(symbol, parameterTypes, supersets, roundingMode));
		translator.roundingMode(roundingMode);
		if (concretizationSettings.decide()) {
			translator.decision(Rule.of(predicate.name(), builder -> {
				var parameters = createParameters(builder);
				var literals = new ArrayList<Literal>(arity + 2);
				literals.add(may(predicate.call(parameters)));
				literals.add(not(candidateMust(predicate.call(parameters))));
				for (int i = 0; i < arity; i++) {
					literals.add(not(MULTI_VIEW.call(parameters[i])));
				}
				builder.clause(literals);
				builder.action(add(predicate, parameters));
			}));
		}
		storeBuilder.with(translator);
	}

	private NodeVariable[] createParameters(AbstractQueryBuilder<?> builder) {
		return TranslatorUtils.createParameters(predicate.arity(), builder);
	}

	private void configureWithDefaultUnknown(PartialRelationTranslator translator) {
		var name = predicate.name();
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);
		int arity = predicate.arity();
		var forbiddenView = new ForbiddenView(symbol);
		var superset = TranslatorUtils.createSupersetHelper(predicate, supersets);
		translator.may(Query.of(mayName, builder -> {
			var parameters = createParameters(builder);
			var literals = new ArrayList<Literal>(arity + 2);
			literals.add(may(superset.call(parameters)));
			for (int i = 0; i < arity; i++) {
				literals.add(may(parameterTypes.get(i).call(parameters[i])));
			}
			literals.add(not(forbiddenView.call(parameters)));
			builder.clause(literals);
		}));
		if (!concretizationSettings.concretize()) {
			var candidateMayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.CANDIDATE);
			translator.candidateMay(Query.of(candidateMayName, builder -> {
				var parameters = createParameters(builder);
				var literals = new ArrayList<Literal>(arity + 2);
				literals.add(candidateMay(superset.call(parameters)));
				for (int i = 0; i < arity; i++) {
					literals.add(candidateMay(parameterTypes.get(i).call(parameters[i])));
				}
				literals.add(not(forbiddenView.call(parameters)));
				builder.clause(literals);
			}));
		}
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = predicate.name();
		var superset = TranslatorUtils.createSupersetHelper(predicate, supersets);
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
		var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
		propagationBuilder.rule(Rule.of(name + "#invalid", builder -> {
			var parameters = createParameters(builder);
			int arity = parameters.length;
			for (int i = 0; i < arity; i++) {
				builder.clause(
						may(predicate.call(parameters)),
						not(may(parameterTypes.get(i).call(parameters[i])))
				);
			}
			builder.clause(
					may(predicate.call(parameters)),
					not(may(superset.call(parameters)))
			);
			builder.action(remove(predicate, parameters));
		}));
		if (concretizationSettings.concretize()) {
			// Predicates concretized by rounding down are already {@code false} in the candidate interpretation,
			// so we don't need to set them to {@code false} manually.
			return;
		}
		propagationBuilder.concretizationRule(Rule.of(name + "#invalidConcretization", builder -> {
			var parameters = createParameters(builder);
			int arity = parameters.length;
			var queryBuilder = Query.builder(name + "#invalidConcretizationPrecondition")
					.parameters(parameters);
			for (int i = 0; i < arity; i++) {
				queryBuilder.clause(
						candidateMay(predicate.call(parameters)),
						not(candidateMay(parameterTypes.get(i).call(parameters[i])))
				);
			}
			queryBuilder.clause(
					may(predicate.call(parameters)),
					not(candidateMay(superset.call(parameters)))
			);
			var literals = new ArrayList<Literal>(arity + 1);
			literals.add(queryBuilder.build().call(parameters));
			for (var parameter : parameters) {
				literals.add(candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
			builder.action(remove(predicate, parameters));
		}));
	}
}
