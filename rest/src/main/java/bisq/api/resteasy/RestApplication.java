package bisq.api.resteasy;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;

@ApplicationPath("/rest")
public class RestApplication extends Application {
    public RestApplication() {
        super();

    }

    protected HashSet<Object> set;

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8082), 10);
        HttpContextBuilder contextBuilder = new HttpContextBuilder();
        contextBuilder.getDeployment().setApplication(new RestApplication());
        contextBuilder.getDeployment().getActualResourceClasses().add(PetResource.class);
//        contextBuilder.getDeployment().getActualResourceClasses().add(AcceptHeaderOpenApiResource.class);
        HttpContext context = contextBuilder.bind(httpServer);
        context.getAttributes().put("some.config.info", "42");
        httpServer.start();

        System.in.read();

        contextBuilder.cleanup();
        httpServer.stop(0);
    }


//    @Override
//    public Set<Object> getSingletons() {
//
//        if (set == null) {
//            set = new HashSet<>() {{
//                add(new HelloWorld());
//            }};
//        }
//        return set;
//    }

}
