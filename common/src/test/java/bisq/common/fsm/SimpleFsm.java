package bisq.common.fsm;

import java.lang.reflect.InvocationTargetException;

public class SimpleFsm<M extends FsmModel> extends Fsm<M> {

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
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return handlerClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public void handle(Event event) {
        try {
            super.handle(event);
        } catch (FsmException fsmException) {
            // We swallow the exception
        }
    }
}
