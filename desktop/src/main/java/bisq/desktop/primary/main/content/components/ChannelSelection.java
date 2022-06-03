package bisq.desktop.primary.main.content.components;

import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.common.utils.Layout;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.channels.PublicTradeChannel;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
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

import java.util.Comparator;

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
            getChannelSelectionModel().sortedList.setComparator(Comparator.comparing(View.ChannelItem::getDisplayString));
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
            filteredList.setPredicate(item -> {
                if (item.getChannel() instanceof PublicTradeChannel publicTradeChannel) {
                    return publicTradeChannel.isVisible();
                } else {
                    return true;
                }
            });
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
                if (listView.lookup(".list-cell") instanceof ListCell listCell) {
                    listView.setPrefHeight(listCell.getHeight() * listView.getItems().size() + 10);
                }
            });
        }

        @EqualsAndHashCode
        @Getter
        static class ChannelItem {
            private final Channel<?> channel;

            public ChannelItem(Channel<?> channel) {
                this.channel = channel;
            }

            public String getDisplayString() {
                if (channel instanceof PublicTradeChannel publicTradeChannel) {
                    return publicTradeChannel.getMarket().map(Market::getCurrencyCodes).orElse(Res.get("tradeChat.addMarketChannel.any"));
                } else {
                    return channel.getDisplayString();
                }
            }
        }
    }
}
