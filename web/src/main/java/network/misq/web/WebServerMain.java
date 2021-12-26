package network.misq.web;


import network.misq.api.DefaultApi;
import network.misq.api.DefaultApplicationFactory;
import network.misq.application.Executable;
import network.misq.application.options.ApplicationOptions;
import network.misq.web.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerMain extends Executable<DefaultApplicationFactory> {
    private static final Logger log = LoggerFactory.getLogger(WebServerMain.class);

    public static void main(String[] args) {
        new WebServerMain(args);
    }

    protected DefaultApi api;
    private WebServer webServer;

    public WebServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationFactory createApplicationFactory(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultApplicationFactory(applicationOptions, args);
    }

    @Override
    protected void createApi() {
        api = new DefaultApi(applicationFactory);
    }

    @Override
    protected void onInitializeDomainCompleted() {
        webServer = new WebServer(api);
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