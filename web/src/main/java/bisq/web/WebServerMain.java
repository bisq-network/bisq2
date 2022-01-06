package bisq.web;


import bisq.application.ApplicationOptions;
import bisq.application.DefaultServiceProvider;
import bisq.application.Executable;
import bisq.web.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerMain extends Executable<DefaultServiceProvider> {
    private static final Logger log = LoggerFactory.getLogger(WebServerMain.class);

    public static void main(String[] args) {
        new WebServerMain(args);
    }

    protected DefaultServiceProvider serviceProvider;
    private WebServer webServer;

    public WebServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultServiceProvider createServiceProvider(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultServiceProvider(applicationOptions, args);
    }

    @Override
    protected void onDomainInitialized() {
        webServer = new WebServer(serviceProvider);
        webServer.start();
    }

    public void shutdown() {
        super.shutdown();

        if (webServer != null) {
            try {
                log.info("Shutting down grpc server...");
                webServer.shutdown();
            } catch (Exception ex) {
                log.error("", ex);
            }
        }
    }
}