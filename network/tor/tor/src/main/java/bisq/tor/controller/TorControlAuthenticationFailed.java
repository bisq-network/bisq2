package bisq.tor.controller;

public class TorControlAuthenticationFailed extends RuntimeException {
    public TorControlAuthenticationFailed(String message) {
        super(message);
    }
}
