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

package bisq.desktop_automation;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopAutomationServerClickTest {
    @BeforeAll
    static void initJavaFxToolkit() {
        new JFXPanel();
    }

    @Test
    void dispatchClickFiresMousePressedReleasedAndClickedForGenericNodes() throws Exception {
        AtomicInteger pressedCounter = new AtomicInteger();
        AtomicInteger releasedCounter = new AtomicInteger();
        AtomicInteger clickedCounter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> fxFailure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Pane pane = new Pane();
                pane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> pressedCounter.incrementAndGet());
                pane.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> releasedCounter.incrementAndGet());
                pane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> clickedCounter.incrementAndGet());

                Group root = new Group(pane);
                new Scene(root, 200, 120);

                assertThat(DesktopAutomationServer.dispatchClick(pane)).isTrue();
            } catch (Throwable throwable) {
                fxFailure.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(fxFailure.get()).isNull();
        assertThat(pressedCounter.get()).isEqualTo(1);
        assertThat(releasedCounter.get()).isEqualTo(1);
        assertThat(clickedCounter.get()).isEqualTo(1);
    }

    @Test
    void dispatchClickRefusesDisabledNodes() {
        AtomicInteger clickedCounter = new AtomicInteger();
        Pane pane = new Pane();
        pane.setDisable(true);
        pane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> clickedCounter.incrementAndGet());
        new Scene(new Group(pane), 200, 120);

        assertThat(DesktopAutomationServer.dispatchClick(pane)).isFalse();
        assertThat(clickedCounter.get()).isZero();
    }

    @Test
    void dispatchClickRefusesInvisibleNodes() {
        AtomicInteger clickedCounter = new AtomicInteger();
        Pane pane = new Pane();
        pane.setVisible(false);
        pane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> clickedCounter.incrementAndGet());
        new Scene(new Group(pane), 200, 120);

        assertThat(DesktopAutomationServer.dispatchClick(pane)).isFalse();
        assertThat(clickedCounter.get()).isZero();
    }

    @Test
    void extractNodeTextOmitsPasswordFieldContent() {
        PasswordField passwordField = new PasswordField();
        passwordField.setText("secret");

        assertThat(DesktopAutomationServer.extractNodeText(passwordField)).isEmpty();
    }

    @Test
    void urlDecodeKeepsMalformedPercentEncodingAsRawText() throws Exception {
        Method urlDecode = DesktopAutomationServer.class.getDeclaredMethod("urlDecode", String.class);
        urlDecode.setAccessible(true);

        assertThat(urlDecode.invoke(null, "chat%2Finput")).isEqualTo("chat/input");
        assertThat(urlDecode.invoke(null, "%ZZ")).isEqualTo("%ZZ");
    }
}
