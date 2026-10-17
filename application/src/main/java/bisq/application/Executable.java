package bisq.application;

import bisq.application.migration.IncompleteMigrationException;
import bisq.common.platform.PlatformUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class Executable<T extends ApplicationService> implements ShutDownHandler {
    // The application services limit their own shutdown to 10 seconds. The shutdown hook waits a bit longer, so that it
    // cannot keep a hanging JVM alive for ever.
    private static final long MAX_SHUTDOWN_HOOK_WAIT_SEC = 20;

    protected final T applicationService;
    // The UI adds handlers while a shutdown hook can already run them
    protected final List<Runnable> shutdownHandlers = new CopyOnWriteArrayList<>();
    protected volatile boolean shutDownStarted;
    // Completes when shutdown(boolean) has shut down the application service and run the shutdown handlers. Null as
    // long as shutdown(boolean) did not run, for example when a failed start exits on its own. It is set before
    // shutDownStarted, so that a thread which sees shutDownStarted from shutdown(boolean) also sees the future.
    @Nullable
    private volatile CompletableFuture<Void> shutdownCompleted;

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
            awaitShutdownCompleted();
        }));

        try {
            applicationService = createApplicationServiceOrExit(args);

            long ts = System.currentTimeMillis();
            applicationService.pruneAllBackups().join();
            log.info("pruneAllBackups took {} ms", System.currentTimeMillis() - ts);

            ts = System.currentTimeMillis();
            applicationService.readAllPersisted().join();
            log.info("readAllPersisted took {} ms", System.currentTimeMillis() - ts);
        } catch (RuntimeException | Error e) {
            // An app can start threads before this, for example the desktop app its JavaFX thread. Such a thread would
            // keep the JVM running after the failed start, so the JVM exits here.
            shutDownStarted = true;
            log.error("Startup failed", e);
            System.err.println("Error: Startup failed: " + e);
            System.exit(PlatformUtils.EXIT_FAILURE);
            throw e;
        }

        // A shutdown hook can have started the shutdown meanwhile, for example on SIGTERM. Starting the app would then
        // initialize services which are being shut down.
        if (shutDownStarted) {
            log.info("Shutdown started during the start. The app does not start.");
            return;
        }
        launchApplication(args);
    }

    protected abstract T createApplicationService(String[] args);

    private T createApplicationServiceOrExit(String[] args) {
        try {
            return createApplicationService(args);
        } catch (InstanceLockException e) {
            // Startup is aborted, thus there is nothing to shut down. We set shutDownStarted to avoid that our
            // shutdown hook triggers a shutdown of the not yet created application service.
            shutDownStarted = true;
            handleInstanceLockFailure(e);
            // handleInstanceLockFailure is expected to terminate the JVM. In case a custom implementation
            // returns, we must not continue with a half initialized application.
            throw e;
        } catch (IncompleteMigrationException e) {
            // A migration failed, and the configuration or the user chose not to start on the data directory, which is
            // not fully migrated. As above, there is nothing to shut down.
            shutDownStarted = true;
            log.error(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            System.exit(PlatformUtils.EXIT_INCOMPLETE_MIGRATION);
            throw e;
        }
    }

    /**
     * Called if we could not establish that we are the only instance using the data directory. The default
     * implementation logs the reason and exits. Applications with a UI override it to inform the user before exiting.
     */
    protected void handleInstanceLockFailure(InstanceLockException exception) {
        // We do not localize the message here. Headless applications are operated from logs and a console, where
        // English is expected. The desktop application overrides this method to show a localized message.
        log.error(exception.getMessage(), exception.getCause());
        System.err.println("Error: " + exception.getMessage());
        System.exit(PlatformUtils.EXIT_FAILURE);
    }

    public void shutdown() {
        shutdown(true);
    }

    /**
     * Shuts down the application service and runs the shutdown handlers, unless a shutdown has already started.
     *
     * @return Completes when the shutdown which this or an earlier call started has completed
     */
    public CompletableFuture<Void> shutdown(boolean callExit) {
        CompletableFuture<Void> completed;
        // The quit button and the shutdown hook can call this at the same time
        synchronized (this) {
            if (shutDownStarted) {
                log.info("shutDown has already started");
                CompletableFuture<Void> startedShutdown = shutdownCompleted;
                return startedShutdown != null ? startedShutdown : CompletableFuture.completedFuture(null);
            }
            completed = new CompletableFuture<>();
            shutdownCompleted = completed;
            shutDownStarted = true;
        }
        try {
            notifyAboutShutdown();
            if (applicationService != null) {
                applicationService.shutdown()
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("Shutdown of the application service failed", throwable);
                            }
                            completeShutdown(completed, callExit);
                        });
            } else {
                completeShutdown(completed, callExit);
            }
        } catch (Exception e) {
            log.error("Exception at shutdown", e);
            completeShutdown(completed, callExit);
        }
        return completed;
    }

    private void completeShutdown(CompletableFuture<Void> completed, boolean callExit) {
        try {
            runShutdownHandlers();
        } finally {
            // Completed before exitJvm, because System.exit waits for the shutdown hook, which waits for this future
            completed.complete(null);
        }
        if (callExit) {
            exitJvm();
        }
    }

    private void runShutdownHandlers() {
        shutdownHandlers.forEach(shutdownHandler -> {
            try {
                shutdownHandler.run();
            } catch (Exception e) {
                log.error("Exception at running shutdownHandler", e);
            }
        });
    }

    // The JVM ends when the shutdown hook returns. So the hook waits for the shutdown which it or another caller
    // started. Without this, on SIGTERM, which the system sends when it shuts down, the JVM ended before the
    // application service had completed its shutdown.
    private void awaitShutdownCompleted() {
        CompletableFuture<Void> completed = shutdownCompleted;
        if (completed == null) {
            return;
        }
        try {
            completed.get(MAX_SHUTDOWN_HOOK_WAIT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Shutdown did not complete within {} seconds. The JVM ends anyway.", MAX_SHUTDOWN_HOOK_WAIT_SEC);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Shutdown failed", e);
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
        // See the check at the end of the constructor. The desktop app calls this later, after it built its UI.
        if (shutDownStarted) {
            return;
        }
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
