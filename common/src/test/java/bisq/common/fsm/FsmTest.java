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
    void testValidStateTransitions() throws FsmException {
        MockModel model = new MockModel(MockState.INIT);
        Fsm fsm = new Fsm(model);

        // No change in data as no handler was defined
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);

        assertEquals(MockState.INIT, fsm.getModel().stateObservable().get());
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().stateObservable().get());
        assertNull(((MockModel) fsm.getModel()).data);

        // Transit with event handler called
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        assertEquals(MockState.S1, fsm.getModel().stateObservable().get());
        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.S2, fsm.getModel().stateObservable().get());
        assertEquals("test2", ((MockModel) fsm.getModel()).data);

        // Different source state, same event.
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.handle(new MockEvent2(model, "test3"));
        assertEquals(MockState.S3, fsm.getModel().stateObservable().get());
        assertEquals("test3", ((MockModel) fsm.getModel()).data);
    }

    @Test
    void testNoStateTransition() throws FsmException {
        // No such event defined: No state change, no handler call
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm fsm1 = new Fsm(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent2(model1, "test1"));
        assertEquals(MockState.INIT, fsm1.getModel().stateObservable().get());
        assertNull(((MockModel) fsm1.getModel()).data);

        // No transition got added
        // If no target state is set we do not create the transition, so no exception is thrown but no transition if
        // found at handle.
        MockModel model2 = new MockModel(MockState.INIT);
        Fsm fsm2 = new Fsm(model2);
        fsm2.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);
        fsm2.handle(new MockEvent1(model2, "test2"));
        assertEquals(MockState.INIT, fsm2.getModel().stateObservable().get());
        assertNull(((MockModel) fsm2.getModel()).data);

        // If source state is already final we do not transit
        MockModel model3 = new MockModel(MockState.COMPLETED);
        Fsm fsm3 = new Fsm(model3);
        fsm3.addTransition()
                .from(MockState.COMPLETED)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.POST);
        fsm3.handle(new MockEvent1(model3, "test3"));
        assertEquals(MockState.COMPLETED, fsm3.getModel().stateObservable().get());
        assertNull(((MockModel) fsm3.getModel()).data);

        // Same event and state combination: No state change, no handler call
        MockModel model4 = new MockModel(MockState.INIT);
        Fsm fsm4 = new Fsm(model4);
        fsm4.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm4.handle(new MockEvent1(model4, "test4"));
        assertEquals(MockState.S1, fsm4.getModel().stateObservable().get());
        assertEquals("test4", ((MockModel) fsm4.getModel()).data);
        fsm4.handle(new MockEvent1(model4, "test5"));
        assertEquals(MockState.S1, fsm4.getModel().stateObservable().get());
        assertEquals("test4", ((MockModel) fsm4.getModel()).data);

        // No state change as wrong event fired
        MockModel model5 = new MockModel(MockState.INIT);
        Fsm fsm5 = new Fsm(model5);
        fsm5.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm5.handle(new MockEvent2(model5, "test4"));
        assertEquals(MockState.INIT, fsm5.getModel().stateObservable().get());
        assertNull(((MockModel) fsm5.getModel()).data);
    }

    @Test
    void testEventHandlerNotCalled() throws FsmException {
        // No EventHandlerClass defined
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm fsm1 = new Fsm(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent1(model1, "test1"));
        assertEquals(MockState.S1, fsm1.getModel().stateObservable().get());
        assertNull(((MockModel) fsm1.getModel()).data);
    }

    @Test
    void testFsmExceptions() throws FsmException {
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(null)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(null)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(null)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(null);
            fsm.handle(new MockEvent1(model, ""));
        });
        // Initial state is null
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(null);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });
        // Same source and target state
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // MockEventHandler2 constructor not matching defined constructor signature in newEventHandlerFromClass
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler2.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // same pair sourceState/event added
        Assertions.assertThrows(FsmException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            Fsm fsm = new Fsm(model);
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

        MockState() {
            this.isFinalState = false;
        }

        MockState(boolean isFinalState) {
            this.isFinalState = isFinalState;
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

    public static class MockEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            if (event instanceof MockEvent1) {
                MockEvent1 mockEvent = (MockEvent1) event;
                mockEvent.model.data = mockEvent.data;
            }
        }
    }

    public static class MockEventHandler2 implements EventHandler {
        public MockEventHandler2(String test) {
        }

        @Override
        public void handle(Event event) {
            if (event instanceof MockEvent1) {
                MockEvent1 mockEvent = (MockEvent1) event;
                mockEvent.model.data = mockEvent.data;
            }
        }
    }

    public static class MockModel extends Model {
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