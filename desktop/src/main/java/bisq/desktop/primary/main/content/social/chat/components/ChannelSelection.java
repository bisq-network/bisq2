package bisq.desktop.primary.main.content.social.chat.components;

import bisq.common.data.ByteArray;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.robohash.RoboHash;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import bisq.social.user.ChatUser;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
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
            sortedList.setComparator(Comparator.comparing(Channel::getChannelName));
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, ChannelSelection.Model, ChannelSelection.Controller> {
        protected final ListView<Channel<?>> listView;
        private final InvalidationListener channelsChangedListener;
        protected Subscription subscription;
        private final ChangeListener<Channel<?>> channelChangeListener;

        protected View(ChannelSelection.Model model, ChannelSelection.Controller controller, String headlineText) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            root.setStyle("-fx-background-color: -bisq-dark-bg;");

            listView = new ListView<>();
            listView.setPrefHeight(100);
            listView.setItems(model.sortedList);
            listView.setFocusTraversable(false);
            listView.setPadding(new Insets(5, 0, 5, 0));
            listView.setStyle("-fx-background-color: -bisq-dark-bg; -fx-border-width: 0;");
            listView.setCellFactory(new Callback<>() {
                @Override
                public ListCell<Channel<?>> call(ListView<Channel<?>> list) {
                    return new ListCell<>() {
                        final BisqLabel label = new BisqLabel();
                        final HBox hBox = new HBox();

                        {
                            hBox.setSpacing(10);
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.setPadding(new Insets(7, 5, 7, 5));
                            hBox.setMouseTransparent(true);
                            setCursor(Cursor.HAND);
                            setStyle("-fx-background-color: -bisq-dark-bg; -fx-border-width: 0;");
                        }

                        @Override
                        public void updateItem(final Channel<?> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                hBox.getChildren().clear();
                                if (item instanceof PrivateChannel privateChannel) {
                                    ChatUser peer = privateChannel.getPeer();
                                    byte[] peersPubKeyHash = peer.getPubKeyHash();
                                    ImageView roboIcon = new ImageView(RoboHash.getImage(new ByteArray(peersPubKeyHash)));
                                    roboIcon.setFitWidth(25);
                                    roboIcon.setFitHeight(25);
                                    hBox.getChildren().add(roboIcon);
                                }
                                hBox.getChildren().add(label);
                                label.setText(item.getChannelName());

                                if (item.equals(model.selectedChannel.get())) {
                                    hBox.setStyle("-fx-background-color: -bisq-dark-bg-selected; -fx-background-radius: 3px");
                                    label.setStyle("-fx-text-fill: -fx-light-text-color;");
                                } else {
                                    hBox.setStyle("-fx-background-color: -bisq-dark-bg; -fx-background-radius: 3px");
                                    label.setStyle("-fx-text-fill: -fx-mid-text-color;");
                                }
                                setGraphic(hBox);
                            } else {
                                hBox.getChildren().clear();
                                setGraphic(null);
                            }
                        }
                    };
                }
            });

            TitledPane titledPane = new TitledPane(headlineText, listView);
            titledPane.setStyle("-fx-background-color: -bisq-dark-bg; -fx-border-width: 0");

            root.getChildren().addAll(titledPane);
            channelChangeListener = (channelObserver, oldValue, newValue) -> {
                if (oldValue == null || oldValue.equals(newValue)) return; // abort endless loop of event triggering
                int indexOfSelectedItem = listView.getItems().indexOf(newValue);
                listView.getSelectionModel().clearAndSelect(indexOfSelectedItem);
                listView.refresh();
            };
            channelsChangedListener = observable -> adjustHeight();
        }

        @Override
        protected void onViewAttached() {
            subscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(), controller::onSelected);
            // We cannot use binding for listView.getSelectionModel().selectedItemProperty() 
            // See: https://stackoverflow.com/questions/32782065/binding-a-javafx-listviews-selection-index-to-an-integer-property#32782145
            model.selectedChannel.addListener(channelChangeListener);

            model.channels.addListener(channelsChangedListener);
            adjustHeight();
        }

        @Override
        protected void onViewDetached() {
            subscription.unsubscribe();
            model.selectedChannel.removeListener(channelChangeListener);
            model.channels.removeListener(channelsChangedListener);
        }

        private void adjustHeight() {
            UIThread.runOnNextRenderFrame(() -> {
                if (listView.lookup(".list-cell") instanceof ListCell listCell) {
                    listView.setPrefHeight(listCell.getHeight() * listView.getItems().size() + 10);
                }
            });
        }

    }
}
