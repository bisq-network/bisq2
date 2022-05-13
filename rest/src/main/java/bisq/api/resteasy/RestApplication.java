package bisq.api.resteasy;

import bisq.application.DefaultApplicationService;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import lombok.Getter;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/rest")
public class RestApplication extends Application {

    @Getter
    protected final DefaultApplicationService applicationService;

    public RestApplication() {
        applicationService = new DefaultApplicationService(new String[]{"--appName=bisq2_API"});
        applicationService.initialize().join();
    }

    private static Set<Class<?>> resourceClasses = new HashSet<>() {{
        add(PetResource.class);
        add(SwaggerResource.class);
    }};
    protected static HttpServer httpServer;
    protected static HttpContextBuilder contextBuilder;

    public static void main(String[] args) throws IOException {
        startServer();
    }

    private static void stopServer() {
        contextBuilder.cleanup();
        httpServer.stop(0);
    }

    private static void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8082), 10);
        httpServer.createContext("/api", new StaticFileHandler("/api/v1/"));
        contextBuilder = new HttpContextBuilder();
        contextBuilder.getDeployment().setApplication(new RestApplication());
        contextBuilder.getDeployment().getActualProviderClasses().add(ProtoWriter.class);
//        contextBuilder.getDeployment().getActualProviderClasses().add(ProtoContextResolver.class);
        HttpContext context = contextBuilder.bind(httpServer);
        httpServer.start();

    }

    @Override
    public Set<Class<?>> getClasses() {
        return resourceClasses;
    }
}
