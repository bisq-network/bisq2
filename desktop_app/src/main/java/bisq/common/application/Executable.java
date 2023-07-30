package bisq.common.application;

import bisq.application.ApplicationService;
import bisq.common.util.OsUtils;
import bisq.desktop.common.application.ShutDownHandler;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Executable<T extends ApplicationService> implements ShutDownHandler {
    protected final T applicationService;
    protected final List<Runnable> shutDownHandlers = new ArrayList<>();

    public Executable(String[] args) {
        setDefaultUncaughtExceptionHandler();
        applicationService = createApplicationService(args);
        applicationService.readAllPersisted().join();
        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    public void shutdown() {
        applicationService.shutdown()
                .thenRun(() -> {
                    shutDownHandlers.forEach(Runnable::run);
                    log.info("Exiting JavaFX Platform");
                    Platform.exit();
                    log.info("Exiting JVM");
                    System.exit(OsUtils.EXIT_SUCCESS);
                });
    }

    @Override
    public void addShutDownHook(Runnable shutDownHandler) {
        shutDownHandlers.add(shutDownHandler);
    }

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
