package bisq.application;

import bisq.common.util.OsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Executable<T extends ApplicationService> implements ShutDownHandler {
    protected final T applicationService;
    protected final List<Runnable> shutDownHandlers = new ArrayList<>();
    protected volatile boolean shutDownStarted;

    public Executable(String[] args) {
        setDefaultUncaughtExceptionHandler();

        // No other shutdown hooks should be used in any client code
        // Using sun.misc.Signal to handle SIGINT events is not recommended as it is an
        // internal API and adds OS specific dependencies.
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        applicationService = createApplicationService(args);
        applicationService.readAllPersisted().join();
        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    public void shutdown() {
        if (shutDownStarted) {
            log.info("shutDown has already started");
            return;
        }
        shutDownStarted = true;
        if (applicationService != null) {
            applicationService.shutdown()
                    .thenRun(() -> {
                        shutDownHandlers.forEach(Runnable::run);
                        exitJvm();
                    });
        }
    }

    protected void exitJvm() {
        log.info("Exiting JVM");
        System.exit(OsUtils.EXIT_SUCCESS);
    }

    @Override
    public void addShutDownHook(Runnable shutDownHandler) {
        shutDownHandlers.add(shutDownHandler);
    }

    protected void launchApplication(String[] args) {
        onApplicationLaunched();
        // For headless applications we block the main thread to not exit. For JavaFX apps the Application.launch call
        // is blocking, thus the keepRunning call is not needed
        keepRunning();
    }

    protected void onApplicationLaunched() {
        applicationService.initialize()
                .whenComplete(this::onApplicationServiceInitialized);
    }

    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
    }

    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("Uncaught exception", throwable));
    }

    protected void keepRunning() {
        try {
            // Avoid that the main thread is exiting
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }
}
