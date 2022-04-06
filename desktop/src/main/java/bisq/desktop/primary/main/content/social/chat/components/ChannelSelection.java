package bisq.desktop.primary.main.content.social.chat.components;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqLabel;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

public abstract class ChannelSelection {
    protected final ChannelSelection.Controller controller;

    public ChannelSelection(ChannelSelection.Controller controller) {
        this.controller = controller;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    protected static abstract class Controller implements bisq.desktop.common.view.Controller {
        protected final ChannelSelection.Model model;
        @Getter
        protected final ChannelSelection.View view;
        protected final ChatService chatService;
        protected Pin selectedChannelPin;
        protected Pin channelsPin;

        protected Controller(ChatService chatService, String headlineText) {
            this.chatService = chatService;

            model = new ChannelSelection.Model();
            view = new ChannelSelection.View(model, this, headlineText);
        }

        @Override
        public void onActivate() {
            selectedChannelPin = FxBindings.subscribe(chatService.getPersistableStore().getSelectedChannel(),
                    channel -> model.selectedChannel.set(model.channels.stream()
                            .filter(currentChannel -> currentChannel.equals(channel))
                            .findAny()
                            .orElse(null))
            );
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            channelsPin.unbind();
        }

        protected void onSelected(Channel<?> channel) {
            if (channel == null) return;
            chatService.selectChannel(channel);
        }
    }


    protected static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        ObservableList<Channel<?>> channels = FXCollections.observableArrayList();
        SortedList<Channel<?>> sortedList = new SortedList<>(channels);

        protected Model() {
            sortedList.sort(Comparator.comparing(Channel::getChannelName));
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, ChannelSelection.Model, ChannelSelection.Controller> {
        protected final ListView<Channel<?>> listView;
        protected Subscription subscription;
        private final ChangeListener<Channel<?>> channelChangeListener;

        protected View(ChannelSelection.Model model, ChannelSelection.Controller controller, String headlineText) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            BisqLabel headline = new BisqLabel(headlineText);
            headline.setPadding(new Insets(0, 0, 0, 10));
            headline.setStyle("-fx-text-fill: -bs-color-green-5; -fx-font-size: 1.4em");

            listView = new ListView<>();
            listView.setItems(model.sortedList);
            listView.setFocusTraversable(false);
            listView.setCellFactory(new Callback<>() {
                @Override
                public ListCell<Channel<?>> call(ListView<Channel<?>> list) {
                    return new ListCell<>() {
                        final BisqLabel label = new BisqLabel();

                        @Override
                        public void updateItem(final Channel item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                label.setText(item.getChannelName());
                                setGraphic(label);
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            });
            root.getChildren().addAll(headline, listView);
            channelChangeListener = (channelObserver, oldValue, newValue) -> {
                if (oldValue == null || oldValue.equals(newValue)) return; // abort endless loop of event triggering
                int indexOfSelectedItem = listView.getItems().indexOf(newValue);
                listView.getSelectionModel().clearAndSelect(indexOfSelectedItem);
            };
        }

        @Override
        protected void onViewAttached() {
            subscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(), controller::onSelected);
            // We cannot use binding for listView.getSelectionModel().selectedItemProperty() 
            // See: https://stackoverflow.com/questions/32782065/binding-a-javafx-listviews-selection-index-to-an-integer-property#32782145
            model.selectedChannel.addListener(channelChangeListener);
        }

        @Override
        protected void onViewDetached() {
            subscription.unsubscribe();
            model.selectedChannel.removeListener(channelChangeListener);
        }
    }
}
