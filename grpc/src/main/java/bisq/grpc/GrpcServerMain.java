package bisq.grpc;


import bisq.application.ApplicationOptions;
import bisq.application.DefaultApplicationService;
import bisq.application.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcServerMain extends Executable<DefaultApplicationService> {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerMain.class);

    public static void main(String[] args) {
        new GrpcServerMain(args);
    }

    protected DefaultApplicationService applicationService;
    private GrpcServer grpcServer;

    public GrpcServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationService createApplicationService(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultApplicationService(applicationOptions, args);
    }

    @Override
    protected void onDomainInitialized() {
        grpcServer = new GrpcServer(applicationService);
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