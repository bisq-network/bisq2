package bisq.common.platform;

public class InvalidVersionException extends RuntimeException {
    public InvalidVersionException(String message) {
        super(message);
    }
}
