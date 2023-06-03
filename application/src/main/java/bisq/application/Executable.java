package bisq.application;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class Executable<T extends ApplicationService> {
    protected final T applicationService;

    public Executable(String[] args) {
        setDefaultUncaughtExceptionHandler();
        applicationService = createApplicationService(args);
        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    protected void launchApplication(String[] args) {
        onApplicationLaunched();
    }

    protected CompletableFuture<Boolean> onApplicationLaunched() {
        return readAllPersisted()
                .thenCompose(success -> {
                    if (success) {
                        return initializeApplicationService();
                    } else {
                        return CompletableFuture.failedFuture(new RuntimeException("Reading persisted data failed."));
                    }
                });
    }

    protected CompletableFuture<Boolean> readAllPersisted() {
        return applicationService.readAllPersisted();
    }

    protected CompletableFuture<Boolean> initializeApplicationService() {
        return applicationService.initialize();
    }

    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> log.error("Uncaught exception", throwable));
    }
}
