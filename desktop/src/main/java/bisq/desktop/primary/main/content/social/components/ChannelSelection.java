package bisq.desktop.primary.main.content.social.components;

import bisq.common.data.ByteArray;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public abstract class ChannelSelection {

    public ChannelSelection() {
    }

    public abstract Pane getRoot();

    protected static abstract class Controller implements bisq.desktop.common.view.Controller {
        protected final ChatService chatService;
        protected Pin selectedChannelPin;
        protected Pin channelsPin;

        protected Controller(ChatService chatService) {
            this.chatService = chatService;
        }

        @Override
        public void onActivate() {
            getChannelSelectionModel().sortedList.setComparator(Comparator.comparing(Channel::getDisplayString));
        }

        protected abstract Model getChannelSelectionModel();

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
    public static abstract class View<M extends  ChannelSelection.Model, C extends ChannelSelection.Controller> extends bisq.desktop.common.view.View<VBox, M, C> {
        protected final ListView<Channel<?>> listView;
        private final InvalidationListener channelsChangedListener;
        protected final Pane titledPaneContainer;
        private final TitledPane titledPane;
        protected Subscription listViewSelectedChannelSubscription, modelSelectedChannelSubscription;

        protected View(M  model, C controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            listView = new ListView<>(model.sortedList);
            listView.setPrefHeight(100);
            listView.setFocusTraversable(false);
            listView.setCellFactory(p -> getListCell());

             titledPane = new TitledPane(getHeadlineText(), listView);

            titledPaneContainer = new Pane();
            titledPaneContainer.getChildren().addAll(titledPane);

            root.getChildren().addAll(titledPaneContainer);
            channelsChangedListener = observable -> adjustHeight();
        }

        protected abstract String getHeadlineText();

        @Override
        protected void onViewAttached() {
            titledPane.prefWidthProperty().bind(root.widthProperty());
            listViewSelectedChannelSubscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(),
                    channel -> {
                        if (channel != null) {
                            controller.onSelected(channel);
                        }
                    });
            modelSelectedChannelSubscription = EasyBind.subscribe(model.selectedChannel,
                    channel -> {
                        if (channel != null && !channel.equals(listView.getSelectionModel().getSelectedItem())) {
                            UIThread.runOnNextRenderFrame(() -> listView.getSelectionModel().select(channel));
                        }
                    });
            model.channels.addListener(channelsChangedListener);
            adjustHeight();
        }

        @Override
        protected void onViewDetached() {
            titledPane.prefWidthProperty().unbind();
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
                            roboIcon.setFitWidth(35);
                            roboIcon.setFitHeight(35);
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
