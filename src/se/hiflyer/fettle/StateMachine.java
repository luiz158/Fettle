package se.hiflyer.fettle;

import se.hiflyer.fettle.util.EnumMultimap;
import se.hiflyer.fettle.util.Multimap;
import se.hiflyer.fettle.util.SetMultimap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StateMachine<T> {
	private T currentState;
	private final Multimap<T, Transition<T>> stateTransitions;
	private final Multimap<T, Runnable> entryActions;
	private final Multimap<T, Runnable> exitActions;

	private StateMachine(T initial, Multimap<T, Transition<T>> stateTransitions, Multimap<T, Runnable> entryActions, Multimap<T, Runnable> exitActions) {
		currentState = initial;
		this.stateTransitions = stateTransitions;
		this.entryActions = entryActions;
		this.exitActions = exitActions;
	}

	public static <T> StateMachine<T> createStateMachine(T initial) {
		return new StateMachine<T>(initial, SetMultimap.<T, Transition<T>>create(),
				  SetMultimap.<T, Runnable>create(), SetMultimap.<T, Runnable>create());
	}

	public static <T extends Enum<T>> StateMachine<T> createStateMachineOfEnum(Class<T> clazz, T initial) {
		return new StateMachine<T>(initial, EnumMultimap.<T, Transition<T>>create(clazz),
				  EnumMultimap.<T, Runnable>create(clazz), EnumMultimap.<T, Runnable>create(clazz));
	}


	public void addTransition(T from, T to, Condition condition, Object event, List<Runnable> actions) {
		stateTransitions.put(from, new Transition<T>(from, to, condition, event, actions));
	}

	public void addTransition(T from, T to, Condition condition, Object event) {
		addTransition(from, to, condition, event, Collections.<Runnable>emptyList());
	}

	public T getCurrentState() {
		return currentState;
	}

	private void moveToNewState(Transition<T> transition) {
		runActions(exitActions, currentState);
		runActions(transition.getTransitionActions());
		currentState = transition.getTo();
		runActions(entryActions, currentState);
	}

	private void runActions(Collection<Runnable> actions) {
		for (Runnable action : actions) {
			action.run();
		}
	}


	private void runActions(Multimap<T, Runnable> actionMap, T state) {
		runActions(actionMap.get(state));
	}

	public void addEntryAction(T entryState, Runnable action) {
		entryActions.put(entryState, action);
	}

	public void addExitAction(T exitState, Runnable action) {
		exitActions.put(exitState, action);
	}

	public void fireEvent(Object event) {
		Collection<Transition<T>> transitions = stateTransitions.get(currentState);
		for (Transition<T> transition : transitions) {
			// TODO: make smart lookup on event instead
			if (transition.getEvent().equals(event)) {
				if (transition.getCondition().isSatisfied()) {
					moveToNewState(transition);
					return;
				}
			}
		}
	}
}
