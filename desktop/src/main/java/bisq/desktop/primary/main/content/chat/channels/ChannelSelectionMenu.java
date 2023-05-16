package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
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

        protected Pin channelsPin, selectedChannelPin;
        protected final Map<String, Pin> seenChatMessageIdsPins = new HashMap<>();
        protected final Map<String, Pin> numChatMessagesPins = new HashMap<>();

        protected Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            chatService = applicationService.getChatService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();

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
                    this::handleSelectedChannelChange);

            chatChannelService.getChannels().forEach(this::addListenersToChannel);
        }

        @Override
        public void onDeactivate() {
            channelsPin.unbind();
            selectedChannelPin.unbind();
            unbindAndClearAllChannelListeners();
        }

        protected void addListenersToChannel(C channel) {
            Pin seenChatMessageIdsPin = channel.getSeenChatMessageIds().addListener(() -> updateUnseenMessagesMap(channel));
            seenChatMessageIdsPins.put(channel.getId(), seenChatMessageIdsPin);
            Pin numChatMessagesPin = channel.getChatMessages().addListener(() -> updateUnseenMessagesMap(channel));
            numChatMessagesPins.put(channel.getId(), numChatMessagesPin);
        }

        protected void removeListenersToChannel(String channelId) {
            seenChatMessageIdsPins.get(channelId).unbind();
            seenChatMessageIdsPins.remove(channelId);
            numChatMessagesPins.get(channelId).unbind();
            numChatMessagesPins.remove(channelId);
        }

        protected void unbindAndClearAllChannelListeners() {
            seenChatMessageIdsPins.values().forEach(Pin::unbind);
            seenChatMessageIdsPins.clear();
            numChatMessagesPins.values().forEach(Pin::unbind);
            numChatMessagesPins.clear();
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

        protected void updateUnseenMessagesMap(ChatChannel<?> chatChannel) {
            UIThread.run(() -> {
                int numUnSeenChatMessages = (int) chatChannel.getChatMessages().stream()
                        .filter(this::isNotMyMessage)
                        .filter(this::isAuthorNotIgnored)
                        .filter(message -> !chatChannel.getSeenChatMessageIds().contains(message.getId()))
                        .count();
                model.channelIdWithNumUnseenMessagesMap.put(chatChannel.getId(), numUnSeenChatMessages);
            });
        }

        protected boolean isNotMyMessage(ChatMessage chatMessage) {
            return chatMessage.isMyMessage(userIdentityService);
        }

        protected boolean isAuthorNotIgnored(ChatMessage chatMessage) {
            return !userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId());
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
            listView.setPrefHeight(40);

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
