package bisq.desktop.primary.main.content.social.chat.components;

import bisq.common.observable.ObservableSet;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.ChatStore;
import bisq.social.chat.PrivateChannel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public abstract class ChannelSelection {
    protected ChannelSelection.Controller controller = null; // must be set in init()

    public ChannelSelection() {
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
        public void onViewAttached() {
            selectedChannelPin = FxBindings.subscribe(chatService.getPersistableStore().getSelectedChannel(),
                    channel -> {
                        model.selectedChannel.set(model.channels.stream()
                            .filter(mych -> mych.id.equals(channel.getId()))
                            .findAny()
                            .orElse(null));
                // TODO
//                        if (channel instanceof PrivateChannel privateChannel) {
//                            model.selectedChannel.set(new ChannelSelection.ListItem(privateChannel));
//                        }
                    });
        }

        @Override
        public void onViewDetached() {
            selectedChannelPin.unbind();
            channelsPin.unbind();
        }

        protected void onSelected(ChannelSelection.ListItem selectedItem) {
            if (selectedItem == null) return;
            chatService.selectChannel(selectedItem.channel);
        }
    }


    protected static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<ChannelSelection.ListItem> selectedChannel = new SimpleObjectProperty<>();
        ObservableList<ChannelSelection.ListItem> channels = FXCollections.observableArrayList();

        protected Model() {
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, ChannelSelection.Model, ChannelSelection.Controller> {
        protected final ListView<ChannelSelection.ListItem> listView;
        protected Subscription subscription;

        protected View(ChannelSelection.Model model, ChannelSelection.Controller controller, String headlineText) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            BisqLabel headline = new BisqLabel(headlineText);
            headline.setPadding(new Insets(0, 0, 0, 10));
            headline.setStyle("-fx-text-fill: -bs-color-green-5; -fx-font-size: 1.4em");

            listView = new ListView<>();
            listView.setItems(model.channels);
            listView.setFocusTraversable(false);
            listView.setCellFactory(new Callback<>() {
                @Override
                public ListCell<ChannelSelection.ListItem> call(ListView<ChannelSelection.ListItem> list) {
                    return new ListCell<>() {
                        final BisqLabel label = new BisqLabel();

                        @Override
                        public void updateItem(final ChannelSelection.ListItem item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                label.setText(item.channelName);
                                setGraphic(label);
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            });
            root.getChildren().addAll(headline, listView);
        }

        @Override
        public void onViewAttached() {
            subscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(), controller::onSelected);
        }

        @Override
        protected void onViewDetached() {
            subscription.unsubscribe();
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem {
        protected final Channel channel;
        protected final String channelName;
        @EqualsAndHashCode.Include
        protected final String id;

        protected ListItem(Channel channel) {
            this.channel = channel;
            channelName = channel.getChannelName();
            id = channel.getId();
        }
    }
}
