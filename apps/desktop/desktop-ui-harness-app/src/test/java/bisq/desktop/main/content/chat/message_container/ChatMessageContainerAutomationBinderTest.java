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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.chat.message_container;

import bisq.chat.ChatChannelDomain;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop_ui_harness_app.DesktopAutomationBinderTestSupport;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatMessageContainerAutomationBinderTest extends DesktopAutomationBinderTestSupport {
    @Test
    void bindsChatMessageContainerSelectorsOutsideProductionView() {
        UserProfileSelection userProfileSelection = mock(UserProfileSelection.class);
        when(userProfileSelection.getRoot()).thenReturn(new Pane());

        ChatMessageContainerView view = new ChatMessageContainerView(
                new ChatMessageContainerModel(ChatChannelDomain.SUPPORT),
                mock(ChatMessageContainerController.class),
                new VBox(),
                new VBox(),
                userProfileSelection);

        assertNoScope(view.getRoot());
        assertNoId(view.messageInput());
        assertNoId(view.sendMessageAction());

        new ChatMessageContainerAutomationBinder().bind(view);

        assertScope(view.getRoot(), "chat-message-container");
        assertId(view.messageInput(), "input");
        assertId(view.sendMessageAction(), "send");
    }
}
