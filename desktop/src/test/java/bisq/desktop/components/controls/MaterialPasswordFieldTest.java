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

package bisq.desktop.components.controls;

import bisq.common.observable.Observable;
import bisq.desktop.common.Transitions;
import bisq.settings.SettingsService;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.mockito.Mockito.when;
import static org.testfx.assertions.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class MaterialPasswordFieldTest {
    @Mock
    SettingsService settingsService;
    AutoCloseable closeable;
    MaterialPasswordField materialPasswordField;

    static {
        //TODO: Set these properties based on the environment (local or CI)
        //https://nofluffjuststuff.com/blog/andres_almiray/2016/02/running_testfx_tests_in_headless_mode
        System.setProperty("testfx.robot", "glass");
        // Set to false to see the robot when running locally. On OSX one need to grant permissions at the privacy settings. 
        // See: https://github.com/TestFX/TestFX/issues/641 
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
    }

    @Start
    void start(Stage stage) {
        closeable = MockitoAnnotations.openMocks(this);
        Transitions.setSettingsService(settingsService);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        materialPasswordField = new MaterialPasswordField();
        stage.setScene(new Scene(new StackPane(materialPasswordField), 100, 100));
        stage.show();
    }

    @AfterEach
    void end() throws Exception {
        closeable.close();
    }

    @Test
    void basicTest(FxRobot robot) {
        textIsPickedUpByPasswordField(robot);
        unmasksTextWhenIconIsClicked(robot);
    }

    private void textIsPickedUpByPasswordField(FxRobot robot) {
        final String passwordSample = "Text sample";
        robot.write(passwordSample);
        assertThat(robot.lookup(".password-field").queryAs(PasswordField.class)).hasText(passwordSample);
    }

    private void unmasksTextWhenIconIsClicked(FxRobot robot) {
        materialPasswordField.showIcon();
        assertThat(materialPasswordField.isMasked()).isTrue();
        robot.clickOn(".icon-button");
        assertThat(materialPasswordField.isMasked()).isFalse();
    }
}