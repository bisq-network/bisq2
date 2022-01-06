package bisq.grpc;


import bisq.application.ApplicationOptions;
import bisq.application.DefaultServiceProvider;
import bisq.application.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcServerMain extends Executable<DefaultServiceProvider> {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerMain.class);

    public static void main(String[] args) {
        new GrpcServerMain(args);
    }

    protected DefaultServiceProvider serviceProvider;
    private GrpcServer grpcServer;

    public GrpcServerMain(String[] args) {
        super(args);
    }

    @Override
    protected DefaultServiceProvider createServiceProvider(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultServiceProvider(applicationOptions, args);
    }

    @Override
    protected void onDomainInitialized() {
        grpcServer = new GrpcServer(serviceProvider);
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