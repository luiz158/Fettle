package se.hiflyer.fettle;

import org.junit.Test;
import se.hiflyer.fettle.impl.MutableTransitionModelImpl;
import se.mockachino.Mockachino;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MovementUsecase {

	private interface State {
	}

	private static enum MovementEvents {
		RELEASED_SPACE, HIT_GROUND, ON_UPDATE, PRESSED_SPACE
	}

	@Test
	public void testMovement() {
		State walking = Mockachino.mock(State.class);
		State jumping = Mockachino.mock(State.class);
		State falling = Mockachino.mock(State.class);
		State crashed = Mockachino.mock(State.class);
		State jetpackthrust = Mockachino.mock(State.class);

		MutableTransitionModelImpl<State, MovementEvents> model = MutableTransitionModelImpl.create(State.class, MovementEvents.class);

		List<Action<State, MovementEvents>> noActions = Collections.<Action<State, MovementEvents>>emptyList();
		model.addTransition(walking, jumping, MovementEvents.PRESSED_SPACE, BasicConditions.ALWAYS, noActions);
		model.addTransition(jumping, falling, MovementEvents.RELEASED_SPACE, BasicConditions.ALWAYS, noActions);
		model.addTransition(falling, jetpackthrust, MovementEvents.PRESSED_SPACE, BasicConditions.ALWAYS, noActions);
		model.addTransition(jetpackthrust, falling, MovementEvents.RELEASED_SPACE, BasicConditions.ALWAYS, noActions);
		model.addTransition(falling, crashed, MovementEvents.HIT_GROUND, BasicConditions.ALWAYS, noActions);
		model.addTransition(crashed, walking, MovementEvents.ON_UPDATE, new Condition() {
			@Override
			public boolean isSatisfied(Arguments args) {
				// wait until body is upright
				return true;
			}
		}, noActions);

		StateMachine<State, MovementEvents> machine = model.newStateMachine(walking);
		assertEquals(walking, machine.getCurrentState());
		machine.fireEvent(MovementEvents.RELEASED_SPACE);
		assertEquals(walking, machine.getCurrentState());
		machine.fireEvent(MovementEvents.PRESSED_SPACE);
		assertEquals(jumping, machine.getCurrentState());
	}


}
