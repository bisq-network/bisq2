/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.chat.message_container;

import bisq.chat.ChatChannelDomain;
import bisq.chat.Citation;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.SubDomain;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.desktop.common.Transitions;
import bisq.desktop.ServiceProvider;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class ChatMessageContainerControllerTest extends TestFxHeadlessSupport {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ServiceProvider serviceProvider;
    @Mock
    private SettingsService settingsService;

    private AutoCloseable closeable;

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
        Transitions.setSettingsService(null);
    }

    @Test
    void supportPublicChannelSendPublishesUnmodifiedText(FxRobot robot) {
        closeable = MockitoAnnotations.openMocks(this);
        Transitions.setSettingsService(settingsService);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));

        when(serviceProvider.getUserService().getUserIdentityService().getUserIdentities())
                .thenReturn(new ObservableSet<>());
        UserProfile userProfile = mock(UserProfile.class);
        UserIdentity selectedUserIdentity = mock(UserIdentity.class);
        when(selectedUserIdentity.getUserProfile()).thenReturn(userProfile);
        when(serviceProvider.getUserService().getUserIdentityService().getSelectedUserIdentity()).thenReturn(selectedUserIdentity);
        when(serviceProvider.getUserService().getBannedUserService().isUserProfileBanned(userProfile)).thenReturn(false);

        CommonPublicChatChannel channel = new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, SubDomain.SUPPORT_SUPPORT);
        TestableController controller = new TestableController(serviceProvider, ChatChannelDomain.SUPPORT);

        robot.interact(() -> {
            controller.selectedChannelChanged(channel);
            controller.onSendMessage("How do I back up my wallet?");
        });

        assertThat(controller.publishedText).isEqualTo("How do I back up my wallet?");
        assertThat(controller.publishedCitation).isEqualTo(Optional.empty());
        assertThat(controller.publishedChannel).isEqualTo(channel);
        assertThat(controller.publishedUserIdentity).isSameAs(selectedUserIdentity);
    }

    private static final class TestableController extends ChatMessageContainerController {
        private String publishedText;
        private Optional<Citation> publishedCitation;
        private CommonPublicChatChannel publishedChannel;
        private UserIdentity publishedUserIdentity;

        private TestableController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
            super(serviceProvider, chatChannelDomain, userProfile -> {
            });
        }

        @Override
        void publishCommonPublicChatMessage(ChatChannelDomain chatChannelDomain,
                                            String text,
                                            Optional<Citation> citation,
                                            CommonPublicChatChannel channel,
                                            UserIdentity userIdentity) {
            publishedText = text;
            publishedCitation = citation;
            publishedChannel = channel;
            publishedUserIdentity = userIdentity;
        }
    }
}
