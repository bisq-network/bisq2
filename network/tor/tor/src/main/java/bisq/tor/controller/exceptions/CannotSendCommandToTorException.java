package bisq.tor.controller.exceptions;

public class CannotSendCommandToTorException extends RuntimeException {
    public CannotSendCommandToTorException(Throwable cause) {
        super(cause);
    }
}
