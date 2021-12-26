package network.misq.grpc;


import network.misq.api.DefaultApi;
import network.misq.api.DefaultApplicationFactory;
import network.misq.application.Executable;
import network.misq.application.options.ApplicationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcServerMain extends Executable<DefaultApplicationFactory> {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerMain.class);

    public static void main(String[] args) {
        new GrpcServerMain(args);
    }

    protected DefaultApi api;
    private GrpcServer grpcServer;

    public GrpcServerMain(String[] args) {
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
        grpcServer = new GrpcServer(api);
        grpcServer.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();

        if (grpcServer != null) {
            grpcServer.shutdown();
        }
    }
}