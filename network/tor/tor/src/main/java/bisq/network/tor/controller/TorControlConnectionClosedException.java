package bisq.network.tor.controller;

public class TorControlConnectionClosedException extends RuntimeException {
    public TorControlConnectionClosedException(String message) {
        super(message);
    }
}
