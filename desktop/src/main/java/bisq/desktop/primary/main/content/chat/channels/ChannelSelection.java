package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.ChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.PrivateChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.trade.channel.PrivateTradeChatChannel;
import bisq.chat.trade.channel.PublicTradeChannel;
import bisq.chat.trade.channel.PublicTradeChannelService;
import bisq.common.observable.Pin;
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
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class ChannelSelection {
    protected static abstract class Controller implements bisq.desktop.common.view.Controller {
        protected final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final UserProfileService userProfileService;
        protected Pin selectedChannelPin;
        protected Pin channelsPin;
        private final Set<Pin> seenChatMessageIdsPins = new HashSet<>();
        private final Set<Pin> numChatMessagesPins = new HashSet<>();

        protected Controller(DefaultApplicationService applicationService) {
            chatService = applicationService.getChatService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
        }

        protected abstract Model getChannelSelectionModel();

        protected abstract ChannelService<?, ?, ?> getChannelService();

        @Override
        public void onActivate() {
            getChannelService().getChannels().forEach(channel -> {
                Pin seenChatMessageIdsPin = channel.getSeenChatMessageIds().addListener(() -> updateUnseenMessagesMap(channel));
                seenChatMessageIdsPins.add(seenChatMessageIdsPin);
                Pin numChatMessagesPin = channel.getChatMessages().addListener(() -> updateUnseenMessagesMap(channel));
                numChatMessagesPins.add(numChatMessagesPin);
            });
        }

        private void updateUnseenMessagesMap(ChatChannel<?> chatChannel) {
            UIThread.run(() -> {
                if (getChannelService() instanceof PublicTradeChannelService) {
                    PublicTradeChannelService publicTradeChannelService = (PublicTradeChannelService) getChannelService();
                    if (!publicTradeChannelService.isVisible((PublicTradeChannel) chatChannel)) {
                        return;
                    }
                }

                int numUnSeenChatMessages = (int) chatChannel.getChatMessages().stream()
                        .filter(this::isNotMyMessage)
                        .filter(this::isAuthorNotIgnored)
                        .filter(message -> !chatChannel.getSeenChatMessageIds().contains(message.getMessageId()))
                        .count();
                getChannelSelectionModel().channelIdWithNumUnseenMessagesMap.put(chatChannel.getId(), numUnSeenChatMessages);
            });
        }

        private boolean isNotMyMessage(ChatMessage chatMessage) {
            return !userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        private boolean isAuthorNotIgnored(ChatMessage chatMessage) {
            return !userProfileService.isChatUserIgnored(chatMessage.getAuthorId());
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            channelsPin.unbind();
            seenChatMessageIdsPins.forEach(Pin::unbind);
            numChatMessagesPins.forEach(Pin::unbind);
        }

        abstract protected void onSelected(View.ChannelItem channelItem);
    }

    protected static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<View.ChannelItem> selectedChannelItem = new SimpleObjectProperty<>();
        View.ChannelItem previousSelectedChannelItem;
        ObservableList<View.ChannelItem> channelItems = FXCollections.observableArrayList();
        FilteredList<View.ChannelItem> filteredList = new FilteredList<>(channelItems);
        SortedList<View.ChannelItem> sortedList = new SortedList<>(filteredList);
        ObservableMap<String, Integer> channelIdWithNumUnseenMessagesMap = FXCollections.observableHashMap();

        public Model() {
        }
    }

    @Slf4j
    public static abstract class View<M extends ChannelSelection.Model, C extends ChannelSelection.Controller> extends bisq.desktop.common.view.View<AnchorPane, M, C> {
        protected final ListView<ChannelItem> listView;
        private final InvalidationListener channelsChangedListener;
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

            listView = new ListView<>(model.sortedList);
            listView.getStyleClass().add("channel-selection-list-view");
            listView.setFocusTraversable(false);
            listView.setCellFactory(p -> getListCell());

            VBox vBox = new VBox(10, headerBox, listView);
            Layout.pinToAnchorPane(vBox, 0, 0, 0, 0);
            root.getChildren().add(vBox);

            channelsChangedListener = observable -> adjustHeight();
        }

        protected abstract String getHeadlineText();

        @Override
        protected void onViewAttached() {
            listViewSelectedChannelSubscription = EasyBind.subscribe(listView.getSelectionModel().selectedItemProperty(),
                    channelItem -> {
                        if (channelItem != null) {
                            if (model.previousSelectedChannelItem != null) {
                                model.previousSelectedChannelItem.setSelected(false);
                            }
                            channelItem.setSelected(true);
                            model.previousSelectedChannelItem = channelItem;
                            controller.onSelected(channelItem);
                        }
                    });
            modelSelectedChannelSubscription = EasyBind.subscribe(model.selectedChannelItem,
                    channelItem -> {
                        if (channelItem == null) {
                            listView.getSelectionModel().clearSelection();
                        } else if (!channelItem.equals(listView.getSelectionModel().getSelectedItem())) {
                            if (model.previousSelectedChannelItem != null) {
                                model.previousSelectedChannelItem.setSelected(false);
                            }
                            channelItem.setSelected(true);
                            model.previousSelectedChannelItem = channelItem;
                            listView.getSelectionModel().select(channelItem);
                        }
                    });
            model.sortedList.addListener(channelsChangedListener);

            adjustHeightCounter = 0;
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
                        UIThread.run(this::adjustHeight);
                    }
                }
            });
        }

        protected void updateCell(ListCell<ChannelItem> cell, ChannelItem item, Label label, ImageView iconImageView) {
            label.setText(item.getDisplayString().toUpperCase());

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

        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        @Getter
        static class ChannelItem {
            @EqualsAndHashCode.Include
            private final String channelName;
            @EqualsAndHashCode.Include
            private final ChannelDomain channelDomain;
            private final ChatChannel<?> chatChannel;
            private String displayString;
            private final boolean hasMultipleProfiles;
            private String iconId;
            private String iconIdHover;
            private String iconIdSelected;

            private boolean isSelected;

            public ChannelItem(ChatChannel<?> chatChannel) {
                this(chatChannel, null);
            }

            public ChannelItem(ChatChannel<?> chatChannel, @Nullable UserIdentityService userIdentityService) {
                this.chatChannel = chatChannel;
                channelDomain = chatChannel.getChannelDomain();
                channelName = chatChannel.getChannelName();
                hasMultipleProfiles = userIdentityService != null && userIdentityService.getUserIdentities().size() > 1;

                String domain = "-" + channelDomain.name().toLowerCase() + "-";
                iconIdSelected = "channels" + domain + channelName;
                iconIdHover = "channels" + domain + channelName + "-white";
                iconId = "channels" + domain + channelName + "-grey";

                if (chatChannel instanceof PrivateChatChannel) {
                    PrivateChatChannel<?> privateChatChannel = (PrivateChatChannel<?>) chatChannel;
                    displayString = privateChatChannel.getDisplayString();
                    // PrivateTradeChannel is handled in ListCell code
                    if (!(chatChannel instanceof PrivateTradeChatChannel) && hasMultipleProfiles) {
                        // If we have more than 1 user profiles we add our profile as well
                        displayString += " [" + privateChatChannel.getMyUserIdentity().getUserName() + "]";
                    }
                } else if (chatChannel instanceof PublicTradeChannel) {
                    displayString = ((PublicTradeChannel) chatChannel).getMarket().getMarketCodes();
                } else {
                    displayString = chatChannel.getDisplayString();
                }
            }

            public void setSelected(boolean selected) {
                isSelected = selected;
            }
        }
    }
}
