package bisq.notifications;

public interface Notification {
    String getId();

    String getTitle();

    String getMessage();
}
