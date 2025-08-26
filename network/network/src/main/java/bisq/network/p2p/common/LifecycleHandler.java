package bisq.network.p2p.common;

public interface LifecycleHandler {
    void initialize();

    void shutdown();
}
