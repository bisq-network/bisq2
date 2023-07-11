package bisq.desktop.main.content.chat.channels;

import bisq.chat.ChatService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.presentation.notifications.NotificationsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class ChannelSelectionMenu<
        C extends ChatChannel<?>,
        S extends ChatChannelService<?, C, ?>,
        E extends ChatChannelSelectionService
        > {

    public ChannelSelectionMenu() {
    }

    public abstract Controller<?, ?, C, S, E> getController();

    public Region getRoot() {
        return getController().getView().getRoot();
    }

    public void deSelectChannel() {
        getController().deSelectChannel();
    }

    protected abstract static class Controller<
            V extends View<M, ?>,
            M extends Model,
            C extends ChatChannel<?>,
            S extends ChatChannelService<?, C, ?>,
            E extends ChatChannelSelectionService
            > implements bisq.desktop.common.view.Controller {

        protected final ChatService chatService;
        protected final UserIdentityService userIdentityService;
        protected final UserProfileService userProfileService;

        protected final M model;
        @Getter
        protected final V view;
        protected final S chatChannelService;
        protected final E chatChannelSelectionService;
        private final NotificationsService notificationsService;
        private final ChatNotificationService chatNotificationService;

        protected Pin channelsPin, selectedChannelPin;
        private final Map<String, NotificationsService.Listener> listenerByChannelId = new HashMap<>();

        protected Controller(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
            chatService = serviceProvider.getChatService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            notificationsService = serviceProvider.getNotificationsService();
            chatNotificationService = chatService.getChatNotificationService();

            chatChannelService = createAndGetChatChannelService(chatChannelDomain);
            chatChannelSelectionService = createAndGetChatChannelSelectionService(chatChannelDomain);
            model = createAndGetModel(chatChannelDomain);
            view = createAndGetView();
        }

        protected abstract S createAndGetChatChannelService(ChatChannelDomain chatChannelDomain);

        protected abstract E createAndGetChatChannelSelectionService(ChatChannelDomain chatChannelDomain);

        protected abstract V createAndGetView();

        protected abstract M createAndGetModel(ChatChannelDomain chatChannelDomain);

        @Override
        public void onActivate() {
            applyPredicate();

            channelsPin = FxBindings.<C, View.ChannelItem>bind(model.channels)
                    .map(this::findOrCreateChannelItem)
                    .to(chatChannelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(chatChannelSelectionService.getSelectedChannel(),
                    channel -> UIThread.run(() -> handleSelectedChannelChange(channel)));

            chatChannelService.getChannels().forEach(this::addNotificationsListenerForChannel);
        }

        @Override
        public void onDeactivate() {
            channelsPin.unbind();
            selectedChannelPin.unbind();
            removeAllNotificationsListeners();
        }

        protected void addNotificationsListenerForChannel(C channel) {
            NotificationsService.Listener listener = notificationId -> updateNumNotifications(channel);
            listenerByChannelId.put(channel.getId(), listener);
            notificationsService.addListener(listener);
        }

        protected void removeNotificationsListenerForChannel(String channelId) {
            NotificationsService.Listener listener = listenerByChannelId.get(channelId);
            if (listener != null) {
                notificationsService.removeListener(listener);
                listenerByChannelId.remove(channelId);
            }
        }

        protected void removeAllNotificationsListeners() {
            listenerByChannelId.values().forEach(notificationsService::removeListener);
            listenerByChannelId.clear();
        }

        protected void handleSelectedChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
            if (isChannelExpectedInstance(chatChannel)) {
                View.ChannelItem channelItem = findOrCreateChannelItem(chatChannel);
                model.selectedChannelItem.set(channelItem);

                if (model.previousSelectedChannelItem != null) {
                    model.previousSelectedChannelItem.setSelected(false);
                }
                model.previousSelectedChannelItem = channelItem;
                channelItem.setSelected(true);
            } else {
                if (model.previousSelectedChannelItem != null) {
                    model.previousSelectedChannelItem.setSelected(false);
                }
                model.selectedChannelItem.set(null);
            }
        }

        protected abstract boolean isChannelExpectedInstance(ChatChannel<? extends ChatMessage> chatChannel);

        public void deSelectChannel() {
            model.getSelectedChannelItem().set(null);
        }

        protected void applyPredicate() {
            model.filteredChannels.setPredicate(item -> true);
        }

        protected void onSelected(ChannelSelectionMenu.View.ChannelItem channelItem) {
            if (channelItem == null) {
                chatChannelSelectionService.selectChannel(null);
            } else {
                chatChannelSelectionService.selectChannel(channelItem.getChatChannel());
            }
        }

        protected void doLeaveChannel(C chatChannel) {
            chatChannelService.leaveChannel(chatChannel);
            chatChannelSelectionService.maybeSelectFirstChannel();
        }

        protected ChannelSelectionMenu.View.ChannelItem findOrCreateChannelItem(ChatChannel<? extends ChatMessage> chatChannel) {
            return model.channels.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getChatChannel().getId().equals(chatChannel.getId()))
                    .findAny()
                    .orElseGet(() -> new View.ChannelItem(chatChannel, chatService.findChatChannelService(chatChannel)));
        }

        protected void updateNumNotifications(C chatChannel) {
            UIThread.run(() -> model.channelIdWithNumUnseenMessagesMap.put(chatChannel.getId(),
                    chatNotificationService.getNumNotificationsByChannel(chatChannel)));
        }
    }

    @Getter
    protected static class Model implements bisq.desktop.common.view.Model {
        private final ChatChannelDomain chatChannelDomain;
        ObjectProperty<View.ChannelItem> selectedChannelItem = new SimpleObjectProperty<>();
        ObservableList<View.ChannelItem> channels = FXCollections.observableArrayList();
        FilteredList<View.ChannelItem> filteredChannels = new FilteredList<>(channels);
        SortedList<View.ChannelItem> sortedChannels = new SortedList<>(filteredChannels);
        ObservableMap<String, Integer> channelIdWithNumUnseenMessagesMap = FXCollections.observableHashMap();
        View.ChannelItem previousSelectedChannelItem;

        public Model(ChatChannelDomain chatChannelDomain) {
            this.chatChannelDomain = chatChannelDomain;
        }
    }

    @Slf4j
    public abstract static class View<
            M extends Model,
            C extends Controller<?, M, ?, ?, ?>
            >
            extends bisq.desktop.common.view.View<AnchorPane, M, C> {
        protected final ListView<ChannelItem> listView;
        private final InvalidationListener filteredListChangedListener;
        protected final HBox headerBox;
        protected Subscription listViewSelectedChannelSubscription, modelSelectedChannelSubscription;
        protected int adjustHeightCounter = 0;

        protected View(M model, C controller) {
            super(new AnchorPane(), model, controller);

            Label headline = new Label(getHeadlineText());
            headline.getStyleClass().add("bisq-text-8");
            HBox.setMargin(headline, new Insets(26, 0, 10, 22));

            // subclasses add settings icon, so we put it into a box
            headerBox = new HBox(20, headline, Spacer.fillHBox());
            headerBox.setMinHeight(54);

            listView = new ListView<>(model.sortedChannels);
            listView.getStyleClass().add("channel-selection-list-view");
            listView.setFocusTraversable(false);
            listView.setCellFactory(p -> getListCell());

            VBox vBox = new VBox(10, headerBox, listView);
            Layout.pinToAnchorPane(vBox, 0, 0, 0, 0);
            root.getChildren().add(vBox);

            filteredListChangedListener = observable -> adjustHeight();
        }

        protected abstract String getHeadlineText();

        @Override
        protected void onViewAttached() {
            listViewSelectedChannelSubscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(),
                    channelItem -> {
                        if (channelItem != null) {
                            controller.onSelected(channelItem);
                        }
                    });
            modelSelectedChannelSubscription = EasyBind.subscribe(model.selectedChannelItem,
                    channelItem -> {
                        if (channelItem == null) {
                            listView.getSelectionModel().clearSelection();
                        } else if (!channelItem.equals(listView.getSelectionModel().getSelectedItem())) {
                            listView.getSelectionModel().select(channelItem);
                        }
                    });
            model.filteredChannels.addListener(filteredListChangedListener);

            adjustHeightCounter = 0;
            adjustHeight();
        }

        @Override
        protected void onViewDetached() {
            listViewSelectedChannelSubscription.unsubscribe();
            modelSelectedChannelSubscription.unsubscribe();
            model.channels.removeListener(filteredListChangedListener);
        }

        protected abstract ListCell<ChannelItem> getListCell();

        private void adjustHeight() {
            adjustHeightCounter++;
            UIThread.runOnNextRenderFrame(() -> {
                Node lookupNode = listView.lookup(".list-cell");
                if (lookupNode instanceof ListCell) {
                    //noinspection rawtypes
                    double height = ((ListCell) lookupNode).getHeight();
                    listView.setPrefHeight(height * listView.getItems().size() + 10);
                    adjustHeightCounter = 10;
                } else {
                    if (!listView.getItems().isEmpty() && adjustHeightCounter < 10) {
                        UIThread.runOnNextRenderFrame(this::adjustHeight);
                    }
                }
            });
        }

        protected void updateCell(ListCell<ChannelItem> cell, ChannelItem item, Label label, ImageView iconImageView) {
            label.setText(item.getChannelTitle().toUpperCase());

            if (item.getIconId() != null) {
                if (item.isSelected) {
                    iconImageView.setId(item.iconIdSelected);
                } else {
                    iconImageView.setId(item.iconId);
                }

                cell.setOnMouseEntered(e -> {
                    if (!item.isSelected) {
                        iconImageView.setId(item.iconIdHover);
                    }
                });
                cell.setOnMouseExited(e -> {
                    if (item.isSelected) {
                        iconImageView.setId(item.iconIdSelected);
                    } else {
                        iconImageView.setId(item.iconId);
                    }
                });

                cell.setOnMousePressed(e -> {
                    iconImageView.setId(item.iconIdSelected);
                });
                cell.setOnMouseReleased(e -> {
                    iconImageView.setId(item.iconIdSelected);
                });
            }
        }

        protected void onUnseenMessagesChanged(ChannelItem item, String channelId, Badge numMessagesBadge) {
            if (channelId.equals(item.getChatChannel().getId())) {
                int numUnseenMessages = model.channelIdWithNumUnseenMessagesMap.get(channelId);
                if (numUnseenMessages > 99) {
                    numMessagesBadge.setText("*");
                } else if (numUnseenMessages > 0) {
                    numMessagesBadge.setText(String.valueOf(numUnseenMessages));
                } else {
                    numMessagesBadge.setText("");
                }
            }
        }

        protected Subscription setupCellBinding(ListCell<ChannelItem> cell, ChannelItem item, Label label, ImageView iconImageView) {
            return EasyBind.subscribe(cell.widthProperty(), w -> {
                if (w.doubleValue() > 0) {
                    label.setMaxWidth(cell.getWidth() - 70);
                }
            });
        }

        //todo create ChannelItems specific to channels
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        @Getter
        static class ChannelItem {
            @EqualsAndHashCode.Include
            private final String channelId;
            private final ChatChannelDomain chatChannelDomain;
            private final ChatChannel<?> chatChannel;
            private final String channelTitle;
            private final String iconId;
            private final String iconIdHover;
            private final String iconIdSelected;

            private boolean isSelected;

            public ChannelItem(ChatChannel<?> chatChannel, Optional<ChatChannelService<?, ?, ?>> chatChannelService) {
                if (chatChannelService.isPresent()) {
                    this.chatChannel = chatChannel;
                    chatChannelDomain = chatChannel.getChatChannelDomain();
                    channelId = chatChannel.getId();
                    channelTitle = chatChannelService.get().getChannelTitle(chatChannel);
                    String styleToken = channelId.replace(".", "-");
                    iconIdSelected = "channels-" + styleToken;
                    iconIdHover = "channels-" + styleToken + "-white";
                    iconId = "channels-" + styleToken + "-grey";
                } else {
                    this.chatChannel = null;
                    chatChannelDomain = null;
                    channelId = "";
                    channelTitle = "";
                    String styleToken = "";
                    iconIdSelected = "channels-" + styleToken;
                    iconIdHover = "channels-" + styleToken + "-white";
                    iconId = "channels-" + styleToken + "-grey";
                }
            }

            public void setSelected(boolean selected) {
                isSelected = selected;
            }
        }
    }
}
