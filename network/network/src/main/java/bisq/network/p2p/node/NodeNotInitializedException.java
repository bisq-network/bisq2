package bisq.network.p2p.node;

public class NodeNotInitializedException extends IllegalStateException {
    public NodeNotInitializedException(String s) {
        super(s);
    }
}
