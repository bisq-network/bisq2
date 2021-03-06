package bisq.desktop.primary.main.content.chat.channels;

import bisq.chat.ChatService;
import bisq.chat.channel.Channel;
import bisq.chat.discuss.pub.PublicDiscussionChannel;
import bisq.chat.events.pub.PublicEventsChannel;
import bisq.chat.support.pub.PublicSupportChannel;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.containers.Spacer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public abstract class ChannelSelection {
    protected static abstract class Controller implements bisq.desktop.common.view.Controller {
        protected final ChatService chatService;

        protected Pin selectedChannelPin;
        protected Pin channelsPin;

        protected Controller(ChatService chatService) {
            this.chatService = chatService;
        }

        @Override
        public void onActivate() {
        }

        protected abstract Model getChannelSelectionModel();

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            if (channelsPin != null) {
                channelsPin.unbind();
            }
        }

        abstract protected void onSelected(View.ChannelItem channelItem);
    }


    protected static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<View.ChannelItem> selectedChannel = new SimpleObjectProperty<>();
        ObservableList<View.ChannelItem> channelItems = FXCollections.observableArrayList();
        FilteredList<View.ChannelItem> filteredList = new FilteredList<>(channelItems);
        SortedList<View.ChannelItem> sortedList = new SortedList<>(filteredList);

        public Model() {
        }
    }

    @Slf4j
    public static abstract class View<M extends ChannelSelection.Model, C extends ChannelSelection.Controller> extends bisq.desktop.common.view.View<AnchorPane, M, C> {
        protected final ListView<ChannelItem> listView;
        private final InvalidationListener channelsChangedListener;
        protected final HBox headerBox;
        protected Subscription listViewSelectedChannelSubscription, modelSelectedChannelSubscription;

        protected View(M model, C controller) {
            super(new AnchorPane(), model, controller);

            Label headline = new Label(getHeadlineText());
            headline.getStyleClass().add("bisq-text-8");
            HBox.setMargin(headline, new Insets(22, 0, 10, 22));

            // subclasses add settings icon, so we put it into a box
            headerBox = new HBox(20, headline, Spacer.fillHBox());

            listView = new ListView<>(model.sortedList);
            listView.getStyleClass().add("channel-selection-list-view");
            listView.setPrefHeight(100);
            listView.setFocusTraversable(false);
            listView.setCellFactory(p -> getListCell());

            VBox vBox = new VBox(10, headerBox, listView);
            vBox.setFillWidth(true);
            Layout.pinToAnchorPane(vBox, 0, 0, 0, 0);
            root.getChildren().add(vBox);
            channelsChangedListener = observable -> adjustHeight();
        }

        protected abstract String getHeadlineText();

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
                        if (channel == null) {
                            listView.getSelectionModel().clearSelection();
                        } else if (!channel.equals(listView.getSelectionModel().getSelectedItem())) {
                            listView.getSelectionModel().select(channel);
                        }
                    });
            model.sortedList.addListener(channelsChangedListener);

            adjustHeight();
        }

        @Override
        protected void onViewDetached() {
            listViewSelectedChannelSubscription.unsubscribe();
            modelSelectedChannelSubscription.unsubscribe();
            model.sortedList.removeListener(channelsChangedListener);
        }

        protected abstract ListCell<ChannelItem> getListCell();

        private void adjustHeight() {
            UIThread.runOnNextRenderFrame(() -> {
                Node lookupNode = listView.lookup(".list-cell");
                if (lookupNode instanceof ListCell) {
                    //noinspection rawtypes
                    listView.setPrefHeight(((ListCell) lookupNode).getHeight() * listView.getItems().size() + 10);
                }
            });
        }

        @EqualsAndHashCode
        @Getter
        static class ChannelItem {
            private final Channel<?> channel;
            private final String displayString;
            private Optional<String> iconId = Optional.empty();

            public ChannelItem(Channel<?> channel) {
                this.channel = channel;

                String type = null;
                if (channel instanceof PublicEventsChannel) {
                    type = "-events-";
                } else if (channel instanceof PublicDiscussionChannel) {
                    type = "-discussion-";
                } else if (channel instanceof PublicSupportChannel) {
                    type = "-support-";
                }
                if (type != null) {
                    iconId = Optional.of("channels" + type + channel.getId());
                }

                if (channel instanceof PublicTradeChannel) {
                    displayString = ((PublicTradeChannel) channel).getMarket().getMarketCodes();
                } else {
                    displayString = channel.getDisplayString();
                }
            }
        }
    }
}
