package bisq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class CompletableFutureUtilsTest {
    @Test
    public void testAllOf() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> cfA = createCompletableFuture(10, "A");
        CompletableFuture<Void> cfB = createCompletableFuture(20, "B");
        CompletableFuture<Void> cfC = createCompletableFuture(30, "C");

        CompletableFutureUtils.allOf(cfA, cfB, cfC)
                .thenApply(list -> {
                    log.info("CompletableFutureUtils.allOf(A, B, C) completed");
                    return null;
                })
                .get();
    }

    @Test
    public void testAnyOf_allSucceed() throws ExecutionException, InterruptedException {

        CompletableFuture<Void> cfA = createCompletableFuture(10, "A");
        CompletableFuture<Void> cfB = createCompletableFuture(20, "B");
        CompletableFuture<Void> cfC = createCompletableFuture(30, "C");

        // Completes as soon as the fastest future in the args
        CompletableFutureUtils.anyOf(cfA, cfB, cfC)
                .thenApply(res -> {
                    log.info("CompletableFutureUtils.anyOf(A, B, C) completed");
                    return null;
                })
                .get();
    }

    @Test
    public void testAnyOfBoolean() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> cfA = createCompletableFutureBool(10, "A", false);
        CompletableFuture<Boolean> cfB = createCompletableFutureBool(20, "B", true);
        CompletableFuture<Boolean> cfC = createCompletableFutureBool(30, "C", true);

        // Completes as soon as the fastest future in the args
        // Has the value returned by the first future that completes
        CompletableFutureUtils.anyOf(cfA, cfB, cfC)
                .thenApply(res -> {
                    log.info("CompletableFutureUtils.anyOf(A, B, C) completed: {}", res);
                    return res;
                })
                .get();
    }

    private CompletableFuture<Boolean> createCompletableFutureBool(long sleepMs, String msg, boolean val) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(sleepMs);
                log.info("{} (waited {} ms: {})", msg, sleepMs, val);
                return val;
            } catch (InterruptedException e) {
                log.error("Interrupted: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    private CompletableFuture<Void> createCompletableFuture(long sleepMs, String msg) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(sleepMs);
                log.info("{} (waited {} ms)", msg, sleepMs);
            } catch (InterruptedException e) {
                log.error("Interrupted: {}", e.getMessage(), e);
            }
        });
    }

    @Test
    public void testAllOfWithType() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(20, 1);
        CompletableFuture<Integer> future_2 = createIntegerFuture(10, 2);
        future_1.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        int sum = CompletableFutureUtils.allOf(future_1, future_2)
                .thenApply(result -> {
                    log.info("CompletableFutureUtils.allOf() completed. result={}", result);
                    return result.stream().mapToInt(e -> e).sum();
                })
                .join();
        assertEquals(3, sum);
    }

    @Test
    public void testAllWithCancel() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(10, 1);
        CompletableFuture<Integer> future_2 = createIntegerFuture(200, 2);
        CompletableFuture<Integer> future_3 = createIntegerFuture(300, 3);
        future_1.whenComplete((result, throwable) -> {
            assertEquals(1, result);
            assertNull(throwable);
            future_2.cancel(true);
        });
        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_3.whenComplete((result, throwable) -> assertNotNull(throwable));

        Exception exception = assertThrows(ExecutionException.class, () ->
                CompletableFutureUtils.allOf(future_1, future_2, future_3).get());
        assertEquals(exception.getCause().getClass(), CancellationException.class);
    }

    @Test
    public void testAllWithException() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(10, 1);
        CompletableFuture<Integer> future_2 = createFailingIntegerFuture(200, 2);
        CompletableFuture<Integer> future_3 = createIntegerFuture(300, 3);
        future_1.whenComplete((result, throwable) -> {
            assertEquals(1, result);
            assertNull(throwable);
        });
        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_3.whenComplete((result, throwable) -> assertNotNull(throwable));

        Exception exception = assertThrows(ExecutionException.class, () ->
                CompletableFutureUtils.allOf(future_1, future_2, future_3).whenComplete((r, t) -> {
        }).get());
        assertEquals(exception.getCause().getClass(), RuntimeException.class);
    }

    @Test
    public void testAnyOfWithType() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(20, 2);
        CompletableFuture<Integer> future_2 = createIntegerFuture(10, 2);
        future_1.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        int result = CompletableFutureUtils.anyOf(future_1, future_2)
                .thenApply(r -> {
                    log.info("CompletableFutureUtils.anyOf() completed. result={}", r);
                    return r;
                })
                .join();
        assertEquals(2, result);
    }

    @Test
    public void testAnyWithCancel() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(10, 1);
        CompletableFuture<Integer> future_2 = createIntegerFuture(200, 3);
        CompletableFuture<Integer> future_3 = createIntegerFuture(300, 3);
        future_1.whenComplete((result, throwable) -> future_2.cancel(true));

        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_3.whenComplete((result, throwable) -> assertNull(throwable));

        CompletableFutureUtils.anyOf(future_2, future_3)
                .whenComplete((r, t) -> assertNull(t))
                .join();
    }

    @Test
    public void testAnyWithException() {
        CompletableFuture<Integer> future_1 = createIntegerFuture(10, 1);
        CompletableFuture<Integer> future_2 = createFailingIntegerFuture(200, 2);
        CompletableFuture<Integer> future_3 = createIntegerFuture(300, 3);
        future_1.whenComplete((result, throwable) -> assertNull(throwable));

        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));
        future_3.whenComplete((result, throwable) -> assertNull(throwable));

        CompletableFutureUtils.anyOf(future_1, future_2, future_3)
                .whenComplete((r, t) -> assertNull(t))
                .join();
    }

    @Test
    public void testAnyWithAllExceptionally() {
        CompletableFuture<Integer> future_1 = createFailingIntegerFuture(10, 1);
        CompletableFuture<Integer> future_2 = createFailingIntegerFuture(20, 2);
        future_1.whenComplete((result, throwable) -> assertNull(throwable));

        future_2.whenComplete((result, throwable) -> assertNotNull(throwable));

        Exception exception = assertThrows(CompletionException.class, () -> CompletableFutureUtils.anyOf(future_1, future_2)
                .whenComplete((r, throwable) -> assertNotNull(throwable))
                .join());
        assertEquals(exception.getCause().getClass(), RuntimeException.class);
    }

    private CompletableFuture<Integer> createIntegerFuture(long sleepMs, int value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(sleepMs);
                return value;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<Integer> createFailingIntegerFuture(long sleepMs, int value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(sleepMs);
                throw new RuntimeException("forced failure");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
