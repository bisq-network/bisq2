package bisq.notifications.system;

public interface Notification {
    String getId();

    String getTitle();

    String getMessage();
}
