package bisq.application.migration.migrations;

public class MigrationFailedException extends RuntimeException {
    public MigrationFailedException(Throwable cause) {
        super(cause);
    }
}
