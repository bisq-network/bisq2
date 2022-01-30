package bisq.grpc;


import bisq.application.DefaultApplicationService;
import bisq.application.Executable;


public class GrpcServerMain extends Executable<DefaultApplicationService> {
    public static void main(String[] args) {
        new GrpcServerMain(args);
    }

    protected DefaultApplicationService applicationService;
    private GrpcServer grpcServer;

    public GrpcServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationService createApplicationService(String[] args) {
        return new DefaultApplicationService(args);
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