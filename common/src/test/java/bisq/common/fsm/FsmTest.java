/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.fsm;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FsmTest {

    @Test
    void testTransitions() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.addTransition()
                .from(MockState.S3)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.COMPLETED);

        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertEquals("test1", model.data);

        fsm.handle(new MockEvent1(model, "test2"));
        assertEquals(MockState.S2, fsm.getModel().getState());
        assertEquals("test2", model.data);

        fsm.handle(new MockEvent1(model, "test3"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", model.data);

        fsm.handle(new MockEvent1(model, "test4"));
        assertEquals(MockState.COMPLETED, fsm.getModel().getState());
        assertEquals("test4", model.data);

        model = new MockModel(MockState.INIT);
        fsm = new SimpleFsm<>(model);

        // No change in data as no handler was defined
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);

        assertEquals(MockState.INIT, fsm.getModel().getState());
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertNull(fsm.getModel().data);

        // Transit with event handler called
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        assertEquals(MockState.S1, fsm.getModel().getState());
        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.S2, fsm.getModel().getState());
        assertEquals("test2", fsm.getModel().data);

        // Different source state, same event.
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.handle(new MockEvent2(model, "test3"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", fsm.getModel().data);
    }

    @Test
    void testOutOfOrderEvents() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent3.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.addTransition()
                .from(MockState.S3)
                .on(MockEvent4.class)
                .run(MockEventHandler.class)
                .to(MockState.COMPLETED);

        fsm.handle(new MockEvent3(model, "test3"));
        assertEquals(MockState.INIT, fsm.getModel().getState());
        assertNull((fsm.getModel()).data);
        assertEquals(1, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());

        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.INIT, fsm.getModel().getState());
        assertNull((fsm.getModel()).data);
        assertEquals(2, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());

        // Now we trigger transition from INIT state and process the queued events
        // to arrive at S3
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", fsm.getModel().data);
        assertEquals(0, model.eventQueue.size());
        assertEquals(3, model.processedEvents.size());

        // At a final state we clear the eventQueue and processedEvents
        fsm.handle(new MockEvent4(model, "test_comp"));
        assertEquals(MockState.COMPLETED, fsm.getModel().getState());
        assertEquals("test_comp", fsm.getModel().data);
        assertEquals(0, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());
    }

    @Test
    void testFromAny() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .fromAny()
                .on(MockEvent2.class)
                .to(MockState.S2);
        fsm.addTransition()
                .fromAny()
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm.addTransition()
                .fromAny()
                .on(MockEvent3.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S1, fsm.getModel().getState());
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());
        fsm.handle(new MockEvent3(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        // Trying to go to a prev state leads to an error
        fsm.handle(new MockEvent1(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testFromStates() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        model = new MockModel(MockState.S1);
        fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        model = new MockModel(MockState.S2);
        fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());
    }

    @Test
    void testErrorState() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        // FailingMockEventHandler thrown an error
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(FailingMockEventHandler.class)
                .to(MockState.S1);
        fsm.handle(new MockEvent1(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testCyclicGraphFailing() {
        MockModel model = new MockModel(MockState.S1);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent1.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S1);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());

        // Going to a lower state is not permitted
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testTransitionToLowerState() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S1);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());

        // Going to a lower state is not permitted (S2->S1)
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testNoStateTransition() {
        // No such event defined: No state change, no handler call
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent2(model1, "test1"));
        assertEquals(State.FsmState.ERROR, fsm1.getModel().getState());
        assertNull((fsm1.getModel()).data);

        // No transition got added
        // If no target state is set we do not create the transition, so no exception is thrown but no transition if
        // found at handle.
        MockModel model2 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm2 = new SimpleFsm<>(model2);
        fsm2.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);
        fsm2.handle(new MockEvent1(model2, "test2"));
        assertEquals(State.FsmState.ERROR, fsm2.getModel().getState());
        assertNull((fsm2.getModel()).data);

        // If source state is already final we do not transit
        MockModel model3 = new MockModel(MockState.COMPLETED);
        Fsm<MockModel> fsm3 = new SimpleFsm<>(model3);
        fsm3.addTransition()
                .from(MockState.COMPLETED)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.POST);
        fsm3.handle(new MockEvent1(model3, "test3"));
        assertEquals(MockState.COMPLETED, fsm3.getModel().getState());
        assertNull((fsm3.getModel()).data);

        // Same event and state combination: No state change, no handler call
        MockModel model4 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm4 = new SimpleFsm<>(model4);
        fsm4.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm4.handle(new MockEvent1(model4, "test4"));
        assertEquals(MockState.S1, fsm4.getModel().getState());
        assertEquals("test4", fsm4.getModel().data);
        fsm4.handle(new MockEvent1(model4, "test5"));
        assertEquals(MockState.S1, fsm4.getModel().getState());
        assertEquals("test4", fsm4.getModel().data);

        // No state change as wrong event fired, no transition found for event
        MockModel model5 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm5 = new SimpleFsm<>(model5);
        fsm5.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm5.handle(new MockEvent2(model5, "test4"));
        assertEquals(State.FsmState.ERROR, fsm5.getModel().getState());
        assertNull((fsm5.getModel()).data);
    }

    @Test
    void testEventHandlerNotCalled() {
        // No EventHandlerClass defined
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent1(model1, "test1"));
        assertEquals(MockState.S1, fsm1.getModel().getState());
        assertNull((fsm1.getModel()).data);
    }

    @Test
    void testInvalidConfigs() {
        // fromStates empty
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .fromStates()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // fromStates null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            //noinspection ConfusingArgumentToVarargsMethod
            fsm.addTransition()
                    .fromStates(null)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // from null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(null)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // missing from
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // on null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(null)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // missing on
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // run null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(null)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // to null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(null);
            fsm.handle(new MockEvent1(model, ""));
        });

        // to missing (not added as transition to fsm). We do not throw an error in the handle method but create an error state
        MockModel model1 = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);
        fsm1.handle(new MockEvent1(model1, ""));
        assertEquals(State.FsmState.ERROR, fsm1.getModel().getState());

        // Initial state is null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(null);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // Same source and target state
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // InvalidMockEventHandler constructor not matching defined constructor signature in newEventHandlerFromClass
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(InvalidMockEventHandler.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // same pair sourceState/event added
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S2);
            fsm.handle(new MockEvent1(model, ""));
        });
    }


    @Getter
    public enum MockState implements State {
        INIT,
        S1,
        S2,
        S3,
        COMPLETED(true),
        POST;
        private final boolean isFinalState;
        private final int ordinal;

        MockState() {
            this(false);
        }

        MockState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    public static class MockEvent1 implements Event {
        private final MockModel model;
        private final String data;

        public MockEvent1(MockModel model, String data) {
            this.model = model;
            this.data = data;
        }
    }

    public static class MockEvent2 extends MockEvent1 {
        public MockEvent2(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEvent3 extends MockEvent1 {
        public MockEvent3(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEvent4 extends MockEvent1 {
        public MockEvent4(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            if (event instanceof MockEvent1) {
                MockEvent1 mockEvent = (MockEvent1) event;
                mockEvent.model.data = mockEvent.data;
            }
        }
    }

    public static class InvalidMockEventHandler implements EventHandler {
        public InvalidMockEventHandler(String test) {
        }

        @Override
        public void handle(Event event) {
            if (event instanceof MockEvent1) {
                MockEvent1 mockEvent = (MockEvent1) event;
                mockEvent.model.data = mockEvent.data;
            }
        }
    }

    public static class FailingMockEventHandler implements EventHandler {
        public FailingMockEventHandler() {
        }

        @Override
        public void handle(Event event) {
            throw new RuntimeException("event is FsmException");
        }
    }

    public static class MockModel extends FsmModel {
        public MockModel(MockState state) {
            super(state);
        }

        public MockModel(MockState state, String data) {
            super(state);
            this.data = data;
        }

        private String data = null;
    }

}