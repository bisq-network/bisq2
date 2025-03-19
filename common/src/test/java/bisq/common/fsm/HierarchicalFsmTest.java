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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class HierarchicalFsmTest {

    private final List<String> eventHandlingSequence = new ArrayList<>();

    @Test
    void testSubFsmActivation() {
        ParentModel parentModel = new ParentModel(ParentState.INIT);
        ParentFsm parentFsm = new ParentFsm(parentModel);

        parentFsm.handle(new StartEvent());
        assertEquals(ParentState.SUB_FSM_STATE, parentModel.getState(),
                "Parent FSM should transition to SUB_FSM_STATE");

        ChildFsm childFsm = parentFsm.getActiveFsm();
        assertNotNull(childFsm, "Child FSM should be activated");
        assertEquals(ChildState.CHILD_INIT, childFsm.getModel().getState(),
                "Child FSM should be in initial state");
    }

    @Test
    void testSubFsmEventRouting() {
        ParentModel parentModel = new ParentModel(ParentState.INIT);
        ParentFsm parentFsm = new ParentFsm(parentModel);

        eventHandlingSequence.clear();

        parentFsm.handle(new StartEvent());
        assertEquals(ParentState.SUB_FSM_STATE, parentModel.getState());

        parentFsm.handle(new ChildEvent());

        ChildFsm childFsm = parentFsm.getActiveFsm();
        assertEquals(ChildState.CHILD_PROCESSING, childFsm.getModel().getState(),
                "Child FSM should have processed the event");

        assertEquals(2, eventHandlingSequence.size(), "Two events should have been handled");
        assertEquals("ParentStartEventHandler", eventHandlingSequence.get(0));
        assertEquals("ChildEventHandler", eventHandlingSequence.get(1));
    }

    @Test
    void testSubFsmCompletionActualBehavior() {
        ParentModel parentModel = new ParentModel(ParentState.INIT);
        ParentFsm parentFsm = new ParentFsm(parentModel);

        parentFsm.handle(new StartEvent());

        parentFsm.handle(new ChildEvent());

        parentFsm.handle(new ChildSuccessEvent());

        assertEquals(State.FsmState.ERROR, parentModel.getState(),
                "Current FSM implementation transitions to ERROR state when child completes");

        if (parentFsm.getActiveFsm() != null) {
            assertTrue(parentFsm.getActiveFsm().getModel().getState().isFinalState(),
                    "Child FSM should be in a final state after completion");
        }
    }

    /**
     * Tests event bubbling from child to parent when child cannot handle an event.
     * In the current FSM implementation, when an event bubbles up:
     * 1. The event is identified as not handleable by the child
     * 2. The event is passed to the parent
     * 3. The parent remains in its current state (with active child)
     * 4. The parent's handler is NOT called (bubbling only affects routing)
     */
    @Test
    void testEventBubbling() {
        ParentModel parentModel = new ParentModel(ParentState.INIT);
        ParentFsm parentFsm = new ParentFsm(parentModel);

        eventHandlingSequence.clear();

        parentFsm.handle(new StartEvent());
        assertEquals(ParentState.SUB_FSM_STATE, parentModel.getState());

        ChildFsm childFsm = parentFsm.getActiveFsm();
        assertNotNull(childFsm, "Child FSM should be activated");
        assertEquals(ChildState.CHILD_INIT, childFsm.getModel().getState());

        int handlerCountBefore = eventHandlingSequence.size();

        // Send a ParentEvent - child FSM has no transition for this event
        // With event bubbling, it should bubble up to the parent FSM
        parentFsm.handle(new ParentEvent());

        // Verify the event bubbled up but did not change parent state and didn't call handler
        assertEquals(ChildState.CHILD_INIT, childFsm.getModel().getState(),
                "Child FSM state should not change");
        assertEquals(ParentState.SUB_FSM_STATE, parentModel.getState(),
                "Parent FSM should remain in SUB_FSM_STATE with active child");

        // Verify no additional handler was called (bubbling only affects routing, not handler execution)
        assertEquals(handlerCountBefore, eventHandlingSequence.size(),
                "No additional event handler should be called during bubbling");
    }

    @Test
    void testParentEventProcessingWhileChildActive() {
        ParentModel parentModel = new ParentModel(ParentState.INIT);
        ParentFsm parentFsm = new ParentFsm(parentModel);

        eventHandlingSequence.clear();

        parentFsm.handle(new StartEvent());
        int handlerCountBefore = eventHandlingSequence.size();

        parentFsm.handle(new ParentEvent());

        assertEquals(ParentState.SUB_FSM_STATE, parentModel.getState(),
                "Parent FSM should remain in SUB_FSM_STATE with active child");

        ChildFsm childFsm = parentFsm.getActiveFsm();
        assertNotNull(childFsm, "Child FSM should still be active");
        assertEquals(ChildState.CHILD_INIT, childFsm.getModel().getState(),
                "Child FSM state should be unchanged");

        assertEquals(handlerCountBefore, eventHandlingSequence.size(),
                "No additional event handler should be called during bubbling");
    }

    /**
     * Tests multi-level event bubbling in nested FSM hierarchies.
     * Events should bubble up through multiple levels if intermediate
     * levels cannot handle them, but should not change states of FSMs
     * that have active children nor execute handlers.
     */
    @Test
    void testMultiLevelEventBubbling() {
        NestedParentModel parentModel = new NestedParentModel(NestedParentState.INIT);
        NestedParentFsm parentFsm = new NestedParentFsm(parentModel);

        eventHandlingSequence.clear();

        parentFsm.handle(new StartEvent());

        parentFsm.handle(new NestedChildEvent());
        int handlerCountBefore = eventHandlingSequence.size();

        NestedChildFsm level1Fsm = parentFsm.getActiveFsm();
        assertNotNull(level1Fsm, "Level 1 FSM should be active");
        assertEquals(NestedChildState.LEVEL2_STATE, level1Fsm.getModel().getState());

        NestedGrandchildFsm level2Fsm = level1Fsm.getActiveFsm();
        assertNotNull(level2Fsm, "Level 2 FSM should be active");

        parentFsm.handle(new TopLevelEvent());

        assertEquals(NestedParentState.LEVEL1_STATE, parentModel.getState(),
                "Parent FSM should remain in LEVEL1_STATE with active child");

        assertNotNull(parentFsm.getActiveFsm(), "Child FSMs should still be active");
        assertEquals(NestedChildState.LEVEL2_STATE, level1Fsm.getModel().getState(),
                "Level 1 FSM should remain in LEVEL2_STATE");
        assertNotNull(level1Fsm.getActiveFsm(), "Level 2 FSM should still be active");
        assertEquals(NestedGrandchildState.INIT, level2Fsm.getModel().getState(),
                "Level 2 FSM should remain in INIT state");

        assertEquals(handlerCountBefore, eventHandlingSequence.size(),
                "No additional event handler should be called during bubbling");
    }

    @Test
    void testNestedFsmCompletionActualBehavior() {
        NestedParentModel parentModel = new NestedParentModel(NestedParentState.INIT);
        NestedParentFsm parentFsm = new NestedParentFsm(parentModel);

        parentFsm.handle(new StartEvent());
        assertEquals(NestedParentState.LEVEL1_STATE, parentModel.getState());

        NestedChildFsm level1Fsm = parentFsm.getActiveFsm();
        assertNotNull(level1Fsm, "Level 1 FSM should be activated");

        parentFsm.handle(new NestedChildEvent());
        assertEquals(NestedChildState.LEVEL2_STATE, level1Fsm.getModel().getState());

        NestedGrandchildFsm level2Fsm = level1Fsm.getActiveFsm();
        assertNotNull(level2Fsm, "Level 2 FSM should be active");

        assertEquals(NestedGrandchildState.INIT, level2Fsm.getModel().getState());

        parentFsm.handle(new NestedGrandchildEvent());
        assertEquals(NestedGrandchildState.PROCESSING, level2Fsm.getModel().getState(),
                "Grandchild FSM should be in PROCESSING state after event");

        parentFsm.handle(new NestedGrandchildCompleteEvent());

        assertEquals(NestedGrandchildState.SUCCESS, level2Fsm.getModel().getState(),
                "Level 2 FSM should reach SUCCESS state after completion event");
        assertTrue(level2Fsm.getModel().getState().isFinalState(),
                "Level 2 FSM should be in a final state after completion");

        parentFsm.handle(new NestedChildCompleteEvent());
    }

    // ================= Parent FSM States and Events =================

    @Getter
    public enum ParentState implements State {
        INIT,
        SUB_FSM_STATE,
        PROCESSING,
        SUCCESS,       // NON-final state - needs to accept further events
        FAILURE(true); // Final state - no further events accepted

        private final boolean isFinalState;
        private final int ordinal;

        ParentState() {
            this(false);
        }

        ParentState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    @Getter
    public class ParentFsm extends Fsm<ParentModel> {
        private ChildFsm activeFsm;

        public ParentFsm(ParentModel model) {
            super(model);
        }

        @Override
        protected void configErrorHandling() {
            fromAny()
                    .on(FsmErrorEvent.class)
                    .to(State.FsmState.ERROR);
        }

        @Override
        protected void configTransitions() {
            from(ParentState.INIT)
                    .on(StartEvent.class)
                    .run(new ParentStartEventHandler())
                    .to(ParentState.SUB_FSM_STATE);

            // This transition is for demonstrating event bubbling
            // It won't be used while a child FSM is active
            from(ParentState.SUB_FSM_STATE)
                    .on(ParentEvent.class)
                    .run(new ParentEventHandler())
                    .to(ParentState.PROCESSING);

            // This transition is for demonstrating state transitions
            // after a child FSM has completed
            from(ParentState.SUCCESS)
                    .on(ParentEvent.class)
                    .run(new ParentEventHandler())
                    .to(ParentState.PROCESSING);

            // Create child FSM and associate it with SUB_FSM_STATE
            ChildModel childModel = new ChildModel(ChildState.CHILD_INIT);
            ChildFsm childFsm = new ChildFsm(childModel);

            // Associate child FSM with SUB_FSM_STATE, defining success and failure transitions
            associateFsm(ParentState.SUB_FSM_STATE, childFsm, ParentState.SUCCESS, ParentState.FAILURE);

            // Store reference to active FSM for test verification
            this.activeFsm = childFsm;
        }

        @Override
        protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            return null;
        }
    }

    public class ParentModel extends FsmModel {
        public ParentModel(ParentState state) {
            super(state);
        }
    }

    // ================= Child FSM States and Events =================

    @Getter
    public enum ChildState implements State {
        CHILD_INIT,
        CHILD_PROCESSING,
        CHILD_SUCCESS(true),
        CHILD_FAILURE(true);

        private final boolean isFinalState;
        private final int ordinal;

        ChildState() {
            this(false);
        }

        ChildState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    public class ChildFsm extends Fsm<ChildModel> {

        public ChildFsm(ChildModel model) {
            super(model);
        }

        @Override
        protected void configErrorHandling() {
            fromAny()
                    .on(FsmErrorEvent.class)
                    .to(ChildState.CHILD_FAILURE);
        }

        @Override
        protected void configTransitions() {
            from(ChildState.CHILD_INIT)
                    .on(ChildEvent.class)
                    .run(new ChildEventHandler())
                    .to(ChildState.CHILD_PROCESSING);

            from(ChildState.CHILD_PROCESSING)
                    .on(ChildSuccessEvent.class)
                    .run(new ChildSuccessEventHandler())
                    .to(ChildState.CHILD_SUCCESS);

            from(ChildState.CHILD_PROCESSING)
                    .on(ChildFailureEvent.class)
                    .run(new ChildFailureEventHandler())
                    .to(ChildState.CHILD_FAILURE);
        }

        @Override
        protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            return null;
        }
    }

    public class ChildModel extends FsmModel {
        public ChildModel(ChildState state) {
            super(state);
        }
    }

    @Getter
    public enum NestedParentState implements State {
        INIT,
        LEVEL1_STATE,
        TOP_LEVEL_PROCESSING,
        SUCCESS,       // NON-final state
        FAILURE(true); // Final state

        private final boolean isFinalState;
        private final int ordinal;

        NestedParentState() {
            this(false);
        }

        NestedParentState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    @Getter
    public enum NestedChildState implements State {
        INIT,
        PROCESSING,
        LEVEL2_STATE,
        SUCCESS(true),
        FAILURE(true);

        private final boolean isFinalState;
        private final int ordinal;

        NestedChildState() {
            this(false);
        }

        NestedChildState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    @Getter
    public enum NestedGrandchildState implements State {
        INIT,
        PROCESSING,
        SUCCESS(true),
        FAILURE(true);

        private final boolean isFinalState;
        private final int ordinal;

        NestedGrandchildState() {
            this(false);
        }

        NestedGrandchildState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    @Getter
    public class NestedParentFsm extends Fsm<NestedParentModel> {
        private NestedChildFsm activeFsm;

        public NestedParentFsm(NestedParentModel model) {
            super(model);
        }

        @Override
        protected void configErrorHandling() {
            fromAny()
                    .on(FsmErrorEvent.class)
                    .to(State.FsmState.ERROR);
        }

        @Override
        protected void configTransitions() {
            from(NestedParentState.INIT)
                    .on(StartEvent.class)
                    .run(new NestedParentStartEventHandler())
                    .to(NestedParentState.LEVEL1_STATE);

            // Add transition for TopLevelEvent - this would be used if no child FSM is active
            from(NestedParentState.LEVEL1_STATE)
                    .on(TopLevelEvent.class)
                    .run(new TopLevelEventHandler())
                    .to(NestedParentState.TOP_LEVEL_PROCESSING);

            NestedChildModel childModel = new NestedChildModel(NestedChildState.INIT);
            NestedChildFsm childFsm = new NestedChildFsm(childModel);

            associateFsm(NestedParentState.LEVEL1_STATE, childFsm,
                    NestedParentState.SUCCESS, NestedParentState.FAILURE);

            this.activeFsm = childFsm;
        }

        @Override
        protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            return null;
        }
    }

    @Getter
    public class NestedChildFsm extends Fsm<NestedChildModel> {
        private NestedGrandchildFsm activeFsm;

        public NestedChildFsm(NestedChildModel model) {
            super(model);
        }

        @Override
        protected void configErrorHandling() {
            fromAny()
                    .on(FsmErrorEvent.class)
                    .to(NestedChildState.FAILURE);
        }

        @Override
        protected void configTransitions() {
            from(NestedChildState.INIT)
                    .on(NestedChildEvent.class)
                    .run(new NestedChildEventHandler())
                    .to(NestedChildState.LEVEL2_STATE);

            from(NestedChildState.PROCESSING)
                    .on(NestedChildCompleteEvent.class)
                    .run(new NestedChildCompleteEventHandler())
                    .to(NestedChildState.SUCCESS);


            NestedGrandchildModel grandchildModel = new NestedGrandchildModel(NestedGrandchildState.INIT);
            NestedGrandchildFsm grandchildFsm = new NestedGrandchildFsm(grandchildModel);

            associateFsm(NestedChildState.LEVEL2_STATE, grandchildFsm,
                    NestedChildState.PROCESSING, NestedChildState.FAILURE);

            this.activeFsm = grandchildFsm;
        }

        @Override
        protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            return null;
        }
    }

    public class NestedGrandchildFsm extends Fsm<NestedGrandchildModel> {
        public NestedGrandchildFsm(NestedGrandchildModel model) {
            super(model);
        }

        @Override
        protected void configErrorHandling() {
            fromAny()
                    .on(FsmErrorEvent.class)
                    .to(NestedGrandchildState.FAILURE);
        }

        @Override
        protected void configTransitions() {
            from(NestedGrandchildState.INIT)
                    .on(NestedGrandchildEvent.class)
                    .run(new NestedGrandchildEventHandler())
                    .to(NestedGrandchildState.PROCESSING);

            from(NestedGrandchildState.PROCESSING)
                    .on(NestedGrandchildCompleteEvent.class)
                    .run(new NestedGrandchildCompleteEventHandler())
                    .to(NestedGrandchildState.SUCCESS);
        }

        @Override
        protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            return null;
        }
    }

    public class NestedParentModel extends FsmModel {
        public NestedParentModel(NestedParentState state) {
            super(state);
        }
    }

    public class NestedChildModel extends FsmModel {
        public NestedChildModel(NestedChildState state) {
            super(state);
        }
    }

    public class NestedGrandchildModel extends FsmModel {
        public NestedGrandchildModel(NestedGrandchildState state) {
            super(state);
        }
    }

    // ================= Events =================

    public class StartEvent implements Event {}

    public class ParentEvent implements Event {}

    public class ChildEvent implements Event {}

    public class ChildSuccessEvent implements Event {}

    public class ChildFailureEvent implements Event {}

    public class NestedChildEvent implements Event {}

    public class NestedChildCompleteEvent implements Event {}

    public class NestedGrandchildEvent implements Event {}

    public class NestedGrandchildCompleteEvent implements Event {}

    public class TopLevelEvent implements Event {}

    // ================= Event Handlers =================

    public class ParentStartEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("ParentStartEventHandler");
        }
    }

    public class ParentEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("ParentEventHandler");
        }
    }

    public class ChildEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("ChildEventHandler");
        }
    }

    public class ChildSuccessEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("ChildSuccessEventHandler");
        }
    }

    public class ChildFailureEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("ChildFailureEventHandler");
        }
    }

    public class NestedParentStartEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("NestedParentStartEventHandler");
        }
    }

    public class NestedChildEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("NestedChildEventHandler");
        }
    }

    public class NestedChildCompleteEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("NestedChildCompleteEventHandler");
        }
    }

    public class NestedGrandchildEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("NestedGrandchildEventHandler");
        }
    }

    public class NestedGrandchildCompleteEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("NestedGrandchildCompleteEventHandler");
        }
    }

    public class TopLevelEventHandler implements EventHandler {
        @Override
        public void handle(Event event) {
            eventHandlingSequence.add("TopLevelEventHandler");
        }
    }
}