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
import bisq.desktop.main.content.chat.message_container.components.ChatMentionPopupMenu;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.user.profile.UserProfile;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
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
    private ChatMessageContainerModel model;
    private ChatMessageContainerView view;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        closeable = MockitoAnnotations.openMocks(this);

        when(userProfileSelection.getRoot()).thenReturn(new Pane());

        model = new ChatMessageContainerModel(ChatChannelDomain.SUPPORT);
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
    void mentionPopupFollowsTheCaretNotTheEndOfTheText(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al hello");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("the popup must open for a mention token in front of existing text")
                .isTrue();

        robot.interact(() -> input.positionCaret(input.getText().length()));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("moving the caret out of the token must hide the popup")
                .isFalse();
    }

    @Test
    void enterCompletesTheMentionInsteadOfSending(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al hello");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onUserProfileSelected(eq(alice), any());
        verify(controller, never()).onSendMessage(any());
        assertThat(view.userMentionPopup().isShowing()).isFalse();
    }

    @Test
    void escapeDismissesThePopupAndEnterSendsAgain(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        verify(controller).onSendMessage("@al");
        verify(controller, never()).onUserProfileSelected(any(), any());
    }

    @Test
    void movingTheCaretIntoAnotherTokenReArmsADismissedPopup(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al x @bo");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // The caret jumps straight from the dismissed token into another one.
        robot.interact(() -> input.positionCaret(9));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("a dismissal must not leak into another mention token")
                .isTrue();
    }

    @Test
    void backspaceWithinTheDismissedTokenKeepsThePopupHidden(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // Backspace produces a torn text/caret intermediate state; the dismissal must survive.
        robot.interact(() -> input.deleteText(2, 3));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("editing the dismissed token must not reopen the popup")
                .isFalse();

        // A genuinely fresh token re-arms it.
        robot.interact(() -> {
            input.setText("");
            input.positionCaret(0);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            input.setText("@a");
            input.positionCaret(2);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("a fresh mention token after leaving the dismissed one must open the popup")
                .isTrue();
    }

    @Test
    void arrowDownMovesTheSelectionAndTabCompletesIt(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile albert = mock(UserProfile.class);
        when(albert.getUserName()).thenReturn("albert");
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(albert), new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isTrue();

        // Both candidates match; they sort alphabetically, so the second ArrowDown lands on alice.
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false)));
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false)));
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onUserProfileSelected(eq(alice), any());
        verify(controller, never()).onSendMessage(any());
        assertThat(view.userMentionPopup().isShowing()).isFalse();
    }

    @Test
    void arrowUpMovesTheSelectionBack(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile albert = mock(UserProfile.class);
        when(albert.getUserName()).thenReturn("albert");
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(albert), new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false)));
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false)));
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, false)));
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onUserProfileSelected(eq(albert), any());
        verify(controller, never()).onSendMessage(any());
    }

    @Test
    void shiftEnterInsertsANewLineWhileThePopupShows(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isTrue();

        // The popup only claims unmodified keys; Shift+Enter keeps its line-break meaning.
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, true, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(input.getText()).isEqualTo("@al" + System.lineSeparator());
        verify(controller, never()).onUserProfileSelected(any(), any());
        verify(controller, never()).onSendMessage(any());
    }

    @Test
    void enterWithoutAMatchingCandidateSendsTheMessage(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@zz");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isTrue();

        // With an empty candidate list the popup lets Enter through to the input field.
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();

        verify(controller).onSendMessage("@zz");
        verify(controller, never()).onUserProfileSelected(any(), any());
    }

    @Test
    void replacingADismissedTokenAtTheSameOffsetReArmsThePopup(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // Select-all and retype puts a fresh token at the same offset in one text change.
        robot.interact(() -> {
            input.selectAll();
            input.replaceSelection("@bo");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("a dismissal must not suppress a fresh token at the same offset")
                .isTrue();
    }

    @Test
    void atomicallyReplacingADismissedTokenReArmsThePopup(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // The edit replaces the dismissed token's indicator, so the dismissed token no
        // longer exists; the fresh one at the same offset re-arms the popup.
        robot.interact(() -> {
            input.selectAll();
            input.replaceSelection("@alice");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("replacing the dismissed token wholesale must reopen the popup")
                .isTrue();
    }

    @Test
    void backspacingToTheBareIndicatorKeepsThePopupHidden(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@a");
            input.positionCaret(2);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // The indicator survives the edit, so this is still the dismissed token.
        robot.interact(() -> input.deleteText(1, 2));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("backspacing within the dismissed token must not reopen the popup")
                .isFalse();
    }

    @Test
    void replacingADismissedTokenWithIdenticalTextReArmsThePopup(FxRobot robot) {
        TextInputControl input = view.messageInput();
        UserProfile alice = mock(UserProfile.class);
        when(alice.getUserName()).thenReturn("alice");
        robot.interact(() -> view.userMentionPopup().getObservableList()
                .setAll(new ChatMentionPopupMenu.ListItem(alice)));

        robot.targetWindow(stage);
        robot.clickOn(input);
        robot.interact(() -> {
            input.setText("@al");
            input.positionCaret(3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() ->
                input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false)));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing()).isFalse();

        // A byte-identical replacement changes neither the text nor the match, so only the
        // edit itself can re-arm and re-show the popup.
        robot.interact(() -> {
            input.selectAll();
            input.replaceSelection("@al");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(view.userMentionPopup().isShowing())
                .as("replacing the dismissed token with identical text must reopen the popup")
                .isTrue();
    }

    @Test
    void repeatedMentionCompletionsKeepMovingTheCaret(FxRobot robot) {
        TextInputControl input = view.messageInput();

        robot.targetWindow(stage);
        robot.clickOn(input);
        // First completion: the controller publishes the completed text and the caret target.
        robot.interact(() -> {
            model.getTextInput().set("@alice ");
            model.getCaretPositionRequest().set(ChatMessageContainerModel.CaretPositionRequest.of(7));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(input.getCaretPosition()).isEqualTo(7);

        // Sending clears the input; the next completion computes the same caret position.
        robot.interact(() -> model.getTextInput().set(""));
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            model.getTextInput().set("@alice ");
            model.getCaretPositionRequest().set(ChatMessageContainerModel.CaretPositionRequest.of(7));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(input.getCaretPosition())
                .as("an equal caret target after a text update must still position the caret")
                .isEqualTo(7);
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
