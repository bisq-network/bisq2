package bisq.common.fsm;

import java.lang.reflect.InvocationTargetException;

public class SimpleFsm<M extends FsmModel> extends Fsm<M> {
    // Test-only invocation counters, used to assert which persist path a given transition routed through
    // (see FsmTest#testPersistOnFinalStateIsUsedInsteadOfPersistWhenReachingFinalState).
    public int persistCallCount = 0;
    public int persistOnFinalStateCallCount = 0;
    // Test-only capture of the FsmException this class otherwise swallows below, so tests can assert on its
    // message content (e.g. that it never renders a full event instance - see
    // FsmTest#testNoTransitionExceptionMessageDoesNotLeakEventContent /
    // #testHandlerExceptionMessageDoesNotLeakEventContent).
    public FsmException lastSwallowedException;

    public SimpleFsm(M model) {
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
    }

    @Override
    protected <E extends Event> EventHandler<E> newEventHandlerFromClass(Class<? extends EventHandler<E>> handlerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return handlerClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public void handle(Event event) {
        try {
            super.handle(event);
        } catch (FsmException fsmException) {
            // We swallow the exception - test-only capture for assertions, see lastSwallowedException above.
            lastSwallowedException = fsmException;
        }
    }

    @Override
    protected void persist() {
        persistCallCount++;
    }

    @Override
    protected void persistOnFinalState() {
        persistOnFinalStateCallCount++;
    }
}
