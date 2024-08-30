package bisq.tor.controller.exceptions;

public class CannotConnectWithTorException extends RuntimeException {
    public CannotConnectWithTorException(Throwable cause) {
        super(cause);
    }
}
