package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Channel {
    @EqualsAndHashCode.Include
    private final String channelId;
    private final ChatChannelDomain chatChannelDomain;
    private final ChatChannel<?> chatChannel;
    private final String channelTitle;
    private final String iconId;
    private final String iconIdHover;
    private final String iconIdSelected;
    private final NavigationTarget navigationTarget;
    private boolean isSelected;

    // Notifications
    private final ChatNotificationService chatNotificationService;
    private final Observable<Long> numNotifications = new Observable<>();
    private final Pin changedChatNotificationPin;

    public Channel(CommonPublicChatChannel chatChannel, CommonPublicChatChannelService chatChannelService,
                   NavigationTarget navigationTarget, ChatNotificationService chatNotificationService) {
        this.chatChannel = chatChannel;
        chatChannelDomain = chatChannel.getChatChannelDomain();
        channelId = chatChannel.getId();
        channelTitle = chatChannelService.getChannelTitle(chatChannel);
        String styleToken = channelId.replace(".", "-");
        iconIdSelected = "channels-" + styleToken;
        iconIdHover = "channels-" + styleToken + "-white";
        iconId = "channels-" + styleToken + "-grey";
        this.navigationTarget = navigationTarget;
        this.chatNotificationService = chatNotificationService;

        updateNumNotifications(chatNotificationService.getChangedNotification().get());
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::updateNumNotifications);
    }

    public void updateNumNotifications(ChatNotification notification) {
        if (notification == null || !notification.getChatChannelId().equals(channelId)) {
            return;
        }
        UIThread.run(() -> numNotifications.set(chatNotificationService.getNumNotifications(channelId)));
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void dispose() {
        changedChatNotificationPin.unbind();
    }
}
