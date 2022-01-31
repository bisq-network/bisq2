package bisq.application;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Executable<T extends ServiceProvider> {
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
                    if (success) {
                        onDomainInitialized();
                    } else {
                        onInitializeDomainFailed(throwable);
                    }
                });
    }

    protected void onInitializeDomainFailed(Throwable throwable) {
        throwable.printStackTrace();
    }

    abstract protected void onDomainInitialized();

    public void shutdown() {
        applicationService.shutdown();
    }

    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception", throwable);
        });
    }
}
