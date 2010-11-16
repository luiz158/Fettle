package se.hiflyer.fettle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import se.hiflyer.fettle.util.EnumMultimap;
import se.hiflyer.fettle.util.Multimap;
import se.hiflyer.fettle.util.SetMultimap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BasicStateMachine<S, E> implements StateMachine<S, E> {
	private S currentState;
	private final Multimap<S, Transition<S, E>> stateTransitions;
	private final Map<E, Transition<S, E>> fromAllTransitions;
	private final Multimap<S, Action> entryActions;
	private final Multimap<S, Action> exitActions;

	private BasicStateMachine(S initial, Multimap<S, Transition<S, E>> stateTransitions, Multimap<S, Action> entryActions, Multimap<S, Action> exitActions) {
		currentState = initial;
		this.stateTransitions = stateTransitions;
		this.entryActions = entryActions;
		this.exitActions = exitActions;
		fromAllTransitions = Maps.newHashMap();
	}

	public static <S, E> StateMachine<S, E> createStateMachine(S initial) {
		return new BasicStateMachine<S, E>(initial, SetMultimap.<S, Transition<S, E>>create(),
				  SetMultimap.<S, Action>create(), SetMultimap.<S, Action>create());
	}

	public static <S extends Enum<S>, E> StateMachine<S, E> createStateMachineOfEnum(Class<S> clazz, S initial) {
		return new BasicStateMachine<S, E>(initial, EnumMultimap.<S, Transition<S, E>>create(clazz),
				  EnumMultimap.<S, Action>create(clazz), EnumMultimap.<S, Action>create(clazz));
	}


	@Override
	public void addFromAllTransition(S to, E event, Condition condition, List<Action> actions) {
		fromAllTransitions.put(event, new Transition<S, E>(null, to, condition, event, actions));
	}

	@Override
	public void addTransition(S from, S to, E event, Condition condition, List<Action> actions) {
		stateTransitions.put(from, new Transition<S, E>(from, to, condition, event, actions));
	}

	@Override
	public S getCurrentState() {
		return currentState;
	}

	private void moveToNewState(Transition<S, E> transition) {
		runActions(exitActions, currentState);
		runActions(transition.getTransitionActions());
		currentState = transition.getTo();
		runActions(entryActions, currentState);
	}

	private void runActions(Collection<Action> actions) {
		for (Action action : actions) {
			action.perform();
		}
	}


	private void runActions(Multimap<S, Action> actionMap, S state) {
		runActions(actionMap.get(state));
	}

	@Override
	public void addEntryAction(S entryState, Action action) {
		entryActions.put(entryState, action);
	}

	@Override
	public void addExitAction(S exitState, Action action) {
		exitActions.put(exitState, action);
	}

	@Override
	public boolean fireEvent(E event) {
		Collection<Transition<S, E>> transitions = stateTransitions.get(currentState);
		for (Transition<S, E> transition : transitions) {
			// TODO: make smart lookup on event instead
			if (transition.getEvent().equals(event)) {
				if (transition.getCondition().isSatisfied()) {
					moveToNewState(transition);
					return true;
				}
			}
		}
		Transition<S, E> fromAllTransition = fromAllTransitions.get(event);
		if (fromAllTransition != null) {
			if (fromAllTransition.getCondition().isSatisfied()) {
				moveToNewState(fromAllTransition);
				return true;
			}
		}
		return false;
	}

	@Override
	public void forceSetState(S forcedState) {
		Transition<S, E> transition = new Transition<S, E>(currentState, forcedState, BasicConditions.ALWAYS, null);
		moveToNewState(transition);
	}
}
