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

package bisq.desktop_ui_harness_app;

import bisq.common.observable.Observable;
import bisq.desktop.automation.DesktopAutomationMetadata;
import bisq.desktop.common.Transitions;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class DesktopAutomationBinderTestSupport {
    private static final AtomicBoolean JAVA_FX_INITIALIZED = new AtomicBoolean();

    @BeforeAll
    protected static void initJavaFxToolkit() {
        if (JAVA_FX_INITIALIZED.compareAndSet(false, true)) {
            new JFXPanel();
            SettingsService settingsService = mock(SettingsService.class);
            when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
            Transitions.setSettingsService(settingsService);
        }
        Res.setAndApplyLanguageTag("en");
    }

    protected static void assertScope(Node node, String scope) {
        assertThat(DesktopAutomationMetadata.getScope(node)).contains(scope);
    }

    protected static void assertNoScope(Node node) {
        assertThat(DesktopAutomationMetadata.getScope(node)).isEmpty();
    }

    protected static void assertId(Node node, String automationId) {
        assertThat(DesktopAutomationMetadata.getId(node)).contains(automationId);
    }

    protected static void assertNoId(Node node) {
        assertThat(DesktopAutomationMetadata.getId(node)).isEmpty();
    }
}
