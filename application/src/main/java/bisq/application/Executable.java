package bisq.application;

import bisq.common.platform.PlatformUtils;
import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Executable<T extends ApplicationService> implements ShutDownHandler {
    protected final T applicationService;
    protected final List<Runnable> shutdownHandlers = new ArrayList<>();
    protected volatile boolean shutDownStarted;

    public Executable(String[] args) {
        setDefaultUncaughtExceptionHandler();

        // No other shutdown hooks should be used in any client code
        // Using sun.misc.Signal to handle SIGINT events is not recommended as it is an
        // internal API and adds OS specific dependencies.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadName.from(this, "shutdownHook");
            if (!shutDownStarted) {
                // We must not call System.exit as otherwise we would hang.
                shutdown(false);
            }
        }));

        applicationService = createApplicationService(args);

        long ts = System.currentTimeMillis();
        applicationService.pruneAllBackups().join();
        log.info("pruneAllBackups took {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        applicationService.readAllPersisted().join();
        log.info("readAllPersisted took {} ms", System.currentTimeMillis() - ts);

        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    public void shutdown() {
        shutdown(true);
    }

    public void shutdown(boolean callExit) {
        if (shutDownStarted) {
            log.info("shutDown has already started");
            return;
        }
        shutDownStarted = true;
        try {
            notifyAboutShutdown();
            if (applicationService != null) {
                applicationService.shutdown()
                        .thenRun(() -> {
                            shutdownHandlers.forEach(shutdownHandler -> {
                                try {
                                    shutdownHandler.run();
                                } catch (Exception e) {
                                    log.error("Exception at running shutdownHandler", e);
                                }
                            });
                            if (callExit) {
                                exitJvm();
                            }
                        });
            } else {
                shutdownHandlers.forEach(shutdownHandler -> {
                    try {
                        shutdownHandler.run();
                    } catch (Exception e) {
                        log.error("Exception at running shutdownHandler", e);
                    }
                });
                if (callExit) {
                    exitJvm();
                }
            }
        } catch (Exception e) {
            log.error("Exception at shutdown", e);
            if (callExit) {
                exitJvm();
            }
        }
    }

    protected void notifyAboutShutdown() {
    }

    protected void exitJvm() {
        log.info("Exiting JVM");
        System.exit(PlatformUtils.EXIT_SUCCESS);
    }

    @Override
    public void addShutDownHook(Runnable shutDownHandler) {
        shutdownHandlers.add(shutDownHandler);
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
                log.error("Uncaught exception:", throwable));
    }

    protected void keepRunning() {
        try {
            // Avoid that the main thread is exiting
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }
}
