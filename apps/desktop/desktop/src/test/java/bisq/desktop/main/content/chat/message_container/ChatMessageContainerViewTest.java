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
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ChatMessageContainerViewTest extends TestFxHeadlessSupport {
    private static final String EMOJI_PROMPT = "\uD83E\uDD16 how do I restore my wallet?";

    @Mock
    private ChatMessageContainerController controller;
    @Mock
    private UserProfileSelection userProfileSelection;

    private AutoCloseable closeable;
    private Stage stage;
    private ChatMessageContainerView view;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        closeable = MockitoAnnotations.openMocks(this);

        when(userProfileSelection.getRoot()).thenReturn(new Pane());

        ChatMessageContainerModel model = new ChatMessageContainerModel(ChatChannelDomain.SUPPORT);
        view = new ChatMessageContainerView(model,
                controller,
                new VBox(),
                new VBox(),
                userProfileSelection);

        stage.setScene(new Scene(view.getRoot(), 640, 480));
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void enterDispatchesPlainMessage(FxRobot robot) {
        TextInputControl input = view.messageInput();

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText(EMOJI_PROMPT);
            input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
        });
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onSendMessage(EMOJI_PROMPT);
        assertThat(input.getText()).isEmpty();
    }

    @Test
    void sendButtonDispatchesTrimmedMessage(FxRobot robot) {
        TextInputControl input = view.messageInput();
        Button sendAction = (Button) view.sendMessageAction();

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> input.setText("   /ai please help   "));
        robot.interact(sendAction::fire);
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onSendMessage("/ai please help");
        assertThat(input.getText()).isEmpty();
    }

}
