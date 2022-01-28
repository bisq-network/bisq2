package bisq.web;


import bisq.application.ApplicationOptions;
import bisq.application.DefaultApplicationService;
import bisq.application.Executable;
import bisq.web.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerMain extends Executable<DefaultApplicationService> {
    private static final Logger log = LoggerFactory.getLogger(WebServerMain.class);

    public static void main(String[] args) {
        new WebServerMain(args);
    }

    protected DefaultApplicationService applicationService;
    private WebServer webServer;

    public WebServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationService createApplicationService(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultApplicationService(applicationOptions, args);
    }

    @Override
    protected void onDomainInitialized() {
        webServer = new WebServer(applicationService);
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