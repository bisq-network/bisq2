package network.misq.grpc;


import network.misq.api.DefaultApi;
import network.misq.application.DefaultApplicationSetup;
import network.misq.application.Executable;
import network.misq.application.ApplicationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcServerMain extends Executable<DefaultApplicationSetup> {
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
    protected DefaultApplicationSetup createApplicationSetup(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultApplicationSetup(applicationOptions, args);
    }

    @Override
    protected void createApi() {
        api = new DefaultApi(applicationSetup);
    }

    @Override
    protected void onDomainInitialized() {
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