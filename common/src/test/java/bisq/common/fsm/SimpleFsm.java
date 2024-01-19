package bisq.common.fsm;

import java.lang.reflect.InvocationTargetException;

public class SimpleFsm<M extends FsmModel> extends Fsm<M> {

    public SimpleFsm(M model) {
        super(model);
    }

    @Override
    protected void configTransitions() {
    }

    @Override
    protected void handleFsmException(FsmException fsmException) {
        handle(fsmException);
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return handlerClass.getDeclaredConstructor().newInstance();
    }
}
