package bisq.common.application;

import bisq.application.ApplicationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Executable<T extends ApplicationService> {
    protected final T applicationService;

    public Executable(String[] args) {
        setDefaultUncaughtExceptionHandler();
        applicationService = createApplicationService(args);
        applicationService.readAllPersisted().join();
        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    protected void launchApplication(String[] args) {
        onApplicationLaunched();
    }

    protected void onApplicationLaunched() {
        applicationService.initialize()
                .whenComplete(this::onApplicationServiceInitialized);
    }

    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
    }

    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> log.error("Uncaught exception", throwable));
    }
}
