package bisq.desktop.main.content.common_chat;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class ChatSearchService {
    private final StringProperty searchText = new SimpleStringProperty();
    private final SimpleObjectProperty<Runnable> onHelpRequested = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Runnable> onInfoRequested = new SimpleObjectProperty<>();

    public void setOnHelpRequested(Runnable action) {
        onHelpRequested.set(action);
    }

    public void setOnInfoRequested(Runnable action) {
        onInfoRequested.set(action);
    }

    public void triggerHelpRequested() {
        if (onHelpRequested.get() != null) {
            onHelpRequested.get().run();
        }
    }

    public void triggerInfoRequested() {
        if (onInfoRequested.get() != null) {
            onInfoRequested.get().run();
        }
    }
}
