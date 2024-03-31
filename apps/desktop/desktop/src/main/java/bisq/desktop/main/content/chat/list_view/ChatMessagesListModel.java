package bisq.desktop.main.content.chat.list_view;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.user.identity.UserIdentityService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Predicate;

@Getter
public class ChatMessagesListModel implements bisq.desktop.common.view.Model {
    private final UserIdentityService userIdentityService;
    private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
    private final ObservableList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> chatMessages = FXCollections.observableArrayList();
    private final FilteredList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> filteredChatMessages = new FilteredList<>(chatMessages);
    private final SortedList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> sortedChatMessages = new SortedList<>(filteredChatMessages);
    private final BooleanProperty isPublicChannel = new SimpleBooleanProperty();
    private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
    private final ChatChannelDomain chatChannelDomain;
    @Setter
    private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> searchPredicate = e -> true;
    @Setter
    private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> BisqEasyOfferDirectionOrOwnerFilterPredicate = e -> true;
    @Setter
    private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> BisqEasyPeerReputationFilterPredicate = e -> true;
    @Setter
    private boolean autoScrollToBottom;
    @Setter
    private int numReadMessages;
    private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty();
    private final StringProperty numUnReadMessages = new SimpleStringProperty();
    private final BooleanProperty showScrolledDownButton = new SimpleBooleanProperty();
    private final BooleanProperty scrollBarVisible = new SimpleBooleanProperty();
    private final DoubleProperty scrollValue = new SimpleDoubleProperty();

    public ChatMessagesListModel(UserIdentityService userIdentityService,
                                 ChatChannelDomain chatChannelDomain) {
        this.userIdentityService = userIdentityService;
        this.chatChannelDomain = chatChannelDomain;
    }

    boolean isMyMessage(ChatMessage chatMessage) {
        return chatMessage.isMyMessage(userIdentityService);
    }
}
