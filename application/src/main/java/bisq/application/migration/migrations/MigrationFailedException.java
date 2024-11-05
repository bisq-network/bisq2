package bisq.application.migration.migrations;

public class MigrationFailedException extends RuntimeException {
    public MigrationFailedException(String message) {
        super(message);
    }

    public MigrationFailedException(Throwable cause) {
        super(cause);
    }
}
