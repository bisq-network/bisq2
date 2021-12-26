package network.misq.web.server;

import io.reactivex.Observable;
import network.misq.api.DefaultApi;
import network.misq.web.server.handler.GetOffersHandler;
import network.misq.web.server.handler.GetVersionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.RequestLogger;
import ratpack.registry.Registry;
import ratpack.rx2.RxRatpack;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;

public class WebServer {
    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    private RatpackServer ratpackServer;

    private final DefaultApi api;

    public WebServer(DefaultApi api) {
        this.api = api;
    }

    public void start() {
        log.info("start");
        RxRatpack.initialize();
        try {
            ServerConfig serverConfig = ServerConfig.of(config -> config
                    .port(5050)
                    .findBaseDir()
            );
            Registry registrySpec = new MisqRegistrySpec(api).build();
            ratpackServer = RatpackServer.start(server -> server
                    .serverConfig(serverConfig)
                    .registry(registrySpec)
                    .handlers(chain -> chain            // Map request paths to request handlers.
                            .all(RequestLogger.ncsa())
                            .get("server-error", ctx -> {
                                Observable.<String>error(new IllegalStateException("Server error from observable"))
                                        .subscribe(s -> {
                                        });
                            })
                            .get("version", ctx -> ctx.get(GetVersionHandler.class).handle(ctx))
                            .get("getoffers", ctx -> ctx.get(GetOffersHandler.class).handle(ctx))
                            .get(ctx -> ctx.render("Welcome to Misq Web"))
                    )
            );
        } catch (Exception ex) {
            log.error("", ex);
            throw new RuntimeException(ex);
        }
    }

    public void shutdown() {
        if (ratpackServer.isRunning()) {
            try {
                log.info("Server shutdown started");
                ratpackServer.stop();
                log.info("Server shutdown complete");
            } catch (Exception ex) {
                log.error("", ex);
            }
        }
    }
}
