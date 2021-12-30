package network.misq.application;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Executable<T extends ApplicationSetup> {
    protected final T applicationSetup;

    public Executable(String[] args) {
        ApplicationOptions applicationOptions = ApplicationOptionsParser.parse(args);
        applicationSetup = createApplicationSetup(applicationOptions, args);
        createApi();
        launchApplication(args);
    }

    abstract protected T createApplicationSetup(ApplicationOptions applicationOptions, String[] args);

    abstract protected void createApi();

    protected void launchApplication(String[] args) {
        onApplicationLaunched();
    }

    protected void onApplicationLaunched() {
        initializeDomain();
    }

    protected void initializeDomain() {
        applicationSetup.initialize()
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
        applicationSetup.shutdown();
    }
}
