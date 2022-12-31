package bisq.application;

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

    abstract protected T createApplicationService(String[] args);

    protected void launchApplication(String[] args) {
        onApplicationLaunched();
    }

    protected void onApplicationLaunched() {
        applicationService.initialize()
                .whenComplete((success, throwable) -> {
                    if (throwable == null) {
                        if (success) {
                            onDomainInitialized();
                        } else {
                            log.error("Initialize applicationService failed", throwable);
                        }
                    } else {
                        onInitializeDomainFailed(throwable);
                    }
                });
    }

    protected void onInitializeDomainFailed(Throwable throwable) {
        log.error("Initialize domain failed", throwable);
    }

    abstract protected void onDomainInitialized();

    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> log.error("Uncaught exception", throwable));
    }
}
