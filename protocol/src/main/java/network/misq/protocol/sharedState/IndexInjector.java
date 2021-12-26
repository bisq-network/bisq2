package network.misq.protocol.sharedState;

import java.util.Map;

/**
 * Utility class for injecting an integer index into the call stack using {@link IndexInjector#call(Callable, int)},
 * to be retrieved anywhere from within the calling method using {@link IndexInjector#getIndex()}.
 */
class IndexInjector {
    private static Map<String, Integer> NAME_MAP =
            Map.of("call0", 0, "call1", 1, "call2", 2, "call3", 3);

    interface Callable<V, T extends Throwable> {
        V call() throws T;
    }

    static <V, T extends Throwable> V call(Callable<V, T> callable, int index) throws T {
        return callAgain(callable, index);
    }

    static int getIndex() {
        return StackWalker.getInstance().walk(s -> s
                .filter(frame -> frame.getClassName().equals(IndexInjector.class.getName()))
                .map(StackWalker.StackFrame::getMethodName)
                .takeWhile(name -> !name.equals("call"))
                .filter(NAME_MAP::containsKey)
                .mapToInt(NAME_MAP::get)
                .reduce(0, (x, y) -> x * 4 + y)
        );
    }

    private static <V, T extends Throwable> V callAgain(Callable<V, T> callable, int index) throws T {
        switch (index & 3) {
            case 0:
                return call0(callable, index >>> 2);
            case 1:
                return call1(callable, index >>> 2);
            case 2:
                return call2(callable, index >>> 2);
            default:
                return call3(callable, index >>> 2);
        }
    }

    private static <V, T extends Throwable> V call0(Callable<V, T> callable, int index) throws T {
        return index == 0 ? callable.call() : callAgain(callable, index);
    }

    private static <V, T extends Throwable> V call1(Callable<V, T> callable, int index) throws T {
        return index == 0 ? callable.call() : callAgain(callable, index);
    }

    private static <V, T extends Throwable> V call2(Callable<V, T> callable, int index) throws T {
        return index == 0 ? callable.call() : callAgain(callable, index);
    }

    private static <V, T extends Throwable> V call3(Callable<V, T> callable, int index) throws T {
        return index == 0 ? callable.call() : callAgain(callable, index);
    }
}
