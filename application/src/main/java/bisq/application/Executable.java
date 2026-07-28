package bisq.application;

import bisq.common.platform.PlatformUtils;
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
            Thread.currentThread().setName("ShutdownHook");
            if (!shutDownStarted) {
                // We must not call System.exit as otherwise we would hang.
                shutdown(false);
            }
        }));

        applicationService = createApplicationServiceOrExit(args);

        long ts = System.currentTimeMillis();
        applicationService.pruneAllBackups().join();
        log.info("pruneAllBackups took {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        applicationService.readAllPersisted().join();
        log.info("readAllPersisted took {} ms", System.currentTimeMillis() - ts);

        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    private T createApplicationServiceOrExit(String[] args) {
        try {
            return createApplicationService(args);
        } catch (AnotherInstanceRunningException e) {
            // Startup is aborted, thus there is nothing to shut down. We set shutDownStarted to avoid that our
            // shutdown hook triggers a shutdown of the not yet created application service.
            shutDownStarted = true;
            handleAnotherInstanceRunning(e);
            // handleAnotherInstanceRunning is expected to terminate the JVM. In case a custom implementation
            // returns, we must not continue with a half initialized application.
            throw e;
        }
    }

    /**
     * Called if another instance already uses the same data directory. The default implementation logs the reason
     * and exits. Applications with a UI override it to inform the user before exiting.
     */
    protected void handleAnotherInstanceRunning(AnotherInstanceRunningException exception) {
        // We do not localize the message here. Headless applications are operated from logs and a console, where
        // English is expected. The desktop application overrides this method to show a localized message.
        log.error(exception.getMessage());
        System.err.println("Error: " + exception.getMessage());
        System.exit(PlatformUtils.EXIT_FAILURE);
    }

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
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at keepRunning method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
    }
}
