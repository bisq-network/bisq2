package network.misq.application;

import lombok.extern.slf4j.Slf4j;
import network.misq.application.options.ApplicationOptions;
import network.misq.application.options.ApplicationOptionsParser;

@Slf4j
public abstract class Executable<T extends ApplicationSetup> {
    protected final T applicationSetup;

    public Executable(String[] args) {
        ApplicationOptions applicationOptions = ApplicationOptionsParser.parse(args);
        applicationSetup = createApplicationSetup(applicationOptions, args);
        createApi();
        launchApplication();
    }

    abstract protected T createApplicationSetup(ApplicationOptions applicationOptions, String[] args);

    abstract protected void createApi();

    protected void launchApplication() {
        onApplicationLaunched();
    }

    protected void onApplicationLaunched() {
        initializeDomain();
    }

    protected void initializeDomain() {
        applicationSetup.initialize()
                .whenComplete((success, throwable) -> {
                    if (success) {
                        onInitializeDomainCompleted();
                    } else {
                        onInitializeDomainFailed(throwable);
                    }
                });
    }

    protected void onInitializeDomainFailed(Throwable throwable) {
        throwable.printStackTrace();
    }

    abstract protected void onInitializeDomainCompleted();

    public void shutdown() {
        applicationSetup.shutdown().whenComplete((__, throwable) -> {
            if (throwable == null) {
                System.exit(0);
            } else {
                log.error("Error at applicationSetup.shutdown.", throwable);
                System.exit(1);
            }
        });
    }
}
