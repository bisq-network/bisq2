package bisq.desktop.primary.main.content.social.components;

import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.BisqPopup;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ChatMentionPopupMenu<T> extends BisqPopup {
    @Setter
    private ToStringConverter<T> itemDisplayConverter;
    
    @Setter
    private Consumer<T> selectionHandler;
    
    @Setter
    private Collection<T> items;
    
    private final StringProperty filter = new SimpleStringProperty(null);

    public ChatMentionPopupMenu(Node owner) {
        getStyleClass().add("chat-mention-popup-menu");
        setAlignment(Alignment.LEFT);
        setContentNode(new VBox());
        
        filterProperty().addListener((ev, prev, current) -> {
            if (current != null) {
                updateItems(current);

                if (prev == null) {
                    show(owner);
                }
            } else {
                hide();
            }
        });
    }

    public StringProperty filterProperty() {
        return filter;
    }
    
    private void updateItems(String filter) {
        List<Button> buttons = items
                .stream()
                .filter(p -> StringUtils.containsIgnoreCase(itemDisplayConverter.toString(p), filter))
                .map(item -> {
                    Button button = new Button(itemDisplayConverter.toString(item));
                    button.getStyleClass().add("chat-mention-popup-menu-item");
                    button.setMaxWidth(Double.MAX_VALUE);

                    button.setOnAction(evt -> {
                        selectionHandler.accept(item);
                        hide();
                    });
                    return button;
                })
                .toList();

        ((VBox) contentNode).getChildren().setAll(buttons);
    }

    public interface ToStringConverter<T> {
        String toString(T object);
    }
}
