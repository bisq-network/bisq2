package bisq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CompletableFutureUtilsTest {

    @Test
    public void testAllOf() throws ExecutionException, InterruptedException {

        CompletableFuture<Void> cfA = createCompletableFuture(1000, "A");
        CompletableFuture<Void> cfB = createCompletableFuture(2000, "B");
        CompletableFuture<Void> cfC = createCompletableFuture(3000, "C");

        CompletableFutureUtils.allOf(cfA, cfB, cfC)
                .thenApply(res -> {
                    log.info("CompletableFutureUtils.allOf(A, B, C) completed");
                    return null;
                })
                .get();
    }

    @Test
    public void testAnyOf_allSucceed() throws ExecutionException, InterruptedException {

        CompletableFuture<Void> cfA = createCompletableFuture(1000, "A");
        CompletableFuture<Void> cfB = createCompletableFuture(2000, "B");
        CompletableFuture<Void> cfC = createCompletableFuture(3000, "C");

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

        CompletableFuture<Boolean> cfA = createCompletableFutureBool(1000, "A", false);
        CompletableFuture<Boolean> cfB = createCompletableFutureBool(2000, "B", true);
        CompletableFuture<Boolean> cfC = createCompletableFutureBool(3000, "C", true);

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
                log.error("Interrupted: " + e.getMessage(), e);
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
                log.error("Interrupted: " + e.getMessage(), e);
            }
//            return null;
        });
    }

}
