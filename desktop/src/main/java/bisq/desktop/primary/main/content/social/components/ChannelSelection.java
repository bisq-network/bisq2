package bisq.desktop.primary.main.content.social.components;

import bisq.common.data.ByteArray;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.robohash.RoboHash;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import bisq.social.user.ChatUser;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
            model.sortedList.setComparator(Comparator.comparing(Channel::getDisplayString));
            selectedChannelPin = FxBindings.subscribe(chatService.getSelectedChannel(),
                    channel -> {
                        if (model.channels.contains(channel)) {
                            model.selectedChannel.set(channel);
                        } else {
                            model.selectedChannel.set(null);
                        }
                    });
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            channelsPin.unbind();
        }

        protected void onSelected(Channel<?> channel) {
            if (channel == null) return;
            chatService.setSelectedChannel(channel);
        }
    }


    protected static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        ObservableList<Channel<?>> channels = FXCollections.observableArrayList();
        SortedList<Channel<?>> sortedList = new SortedList<>(channels);

        protected Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, ChannelSelection.Model, ChannelSelection.Controller> {
        protected final ListView<Channel<?>> listView;
        private final InvalidationListener channelsChangedListener;
        protected Subscription listViewSelectedChannelSubscription, modelSelectedChannelSubscription;

        protected View(ChannelSelection.Model model, ChannelSelection.Controller controller, String headlineText) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            listView = new ListView<>(model.sortedList);
            listView.setPrefHeight(100);
            listView.setFocusTraversable(false);
            listView.setCellFactory(p -> getListCell());

            TitledPane titledPane = new TitledPane(headlineText, listView);

            root.getChildren().addAll(titledPane);
            channelsChangedListener = observable -> adjustHeight();
        }

        @Override
        protected void onViewAttached() {
            listViewSelectedChannelSubscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(),
                    channel -> {
                        if (channel != null) {
                            controller.onSelected(channel);
                        }
                    });
            modelSelectedChannelSubscription = EasyBind.subscribe(model.selectedChannel,
                    channel -> {
                        if (channel != null) {
                            UIThread.runOnNextRenderFrame(() -> listView.getSelectionModel().clearAndSelect(listView.getItems().indexOf(channel)));
                        } else {
                            UIThread.runOnNextRenderFrame(() -> listView.getSelectionModel().clearSelection());
                        }
                    });
            model.channels.addListener(channelsChangedListener);
            adjustHeight();
        }

        @Override
        protected void onViewDetached() {
            listViewSelectedChannelSubscription.unsubscribe();
            modelSelectedChannelSubscription.unsubscribe();
            model.channels.removeListener(channelsChangedListener);
        }

        protected ListCell<Channel<?>> getListCell() {
            return new ListCell<>() {
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.setMouseTransparent(true);
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));
                }

                @Override
                protected void updateItem(Channel<?> item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        hBox.getChildren().clear();
                        if (item instanceof PrivateChannel privateChannel) {
                            ChatUser peer = privateChannel.getPeer();
                            ImageView roboIcon = new ImageView(RoboHash.getImage(new ByteArray(peer.getPubKeyHash())));
                            roboIcon.setFitWidth(37.5);
                            roboIcon.setFitHeight(37.5);
                            hBox.getChildren().add(roboIcon);
                            String userName = peer.getUserName();
                            label.setText(StringUtils.truncate(userName, 20));
                            label.setTooltip(new Tooltip(peer.getTooltipString()));
                        } else {
                            label.setText(item.getId());
                        }
                        hBox.getChildren().add(label);
                        setGraphic(hBox);
                    } else {
                        hBox.getChildren().clear();
                        setGraphic(null);
                    }
                }
            };
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
