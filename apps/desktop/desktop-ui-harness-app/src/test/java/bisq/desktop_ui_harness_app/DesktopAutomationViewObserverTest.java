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
 * You should have received a copy of the GNU Affero General Public
 * License along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop_ui_harness_app;

import bisq.chat.ChatChannelDomain;
import bisq.desktop.automation.DesktopAutomationMetadata;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerModel;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerView;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.embed.swing.JFXPanel;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DesktopAutomationViewObserverTest {
    @Mock
    private ChatMessageContainerController controller;
    @Mock
    private UserProfileSelection userProfileSelection;

    private AutoCloseable closeable;

    @BeforeAll
    static void initJavaFxToolkit() {
        new JFXPanel();
        Res.setAndApplyLanguageTag("en");
    }

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(userProfileSelection.getRoot()).thenReturn(new Pane());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void bindsChatMessageContainerMetadataOutsideProductionView() {
        ChatMessageContainerView view = new ChatMessageContainerView(
                new ChatMessageContainerModel(ChatChannelDomain.SUPPORT),
                controller,
                new VBox(),
                new VBox(),
                userProfileSelection);

        assertThat(DesktopAutomationMetadata.getScope(view.getRoot())).isEmpty();
        assertThat(DesktopAutomationMetadata.getId(view.getInputField())).isEmpty();
        assertThat(DesktopAutomationMetadata.getId(view.getSendButton())).isEmpty();

        new DesktopAutomationViewObserver().onViewAttached(view);

        assertThat(DesktopAutomationMetadata.getScope(view.getRoot())).contains("chat-message-container");
        assertThat(DesktopAutomationMetadata.getId(view.getInputField())).contains("input");
        assertThat(DesktopAutomationMetadata.getId(view.getSendButton())).contains("send");
    }
}
