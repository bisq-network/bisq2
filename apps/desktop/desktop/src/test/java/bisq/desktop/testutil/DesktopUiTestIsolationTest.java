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


package bisq.desktop.testutil;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class DesktopUiTestIsolationTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "false")
    void unfilteredJUnitRunDoesNotStartUiTestsOrCreateRobots(@Nullable String optIn) {
        String previousOptIn = System.getProperty("bisq.uiTest");
        String previousRobot = System.getProperty("testfx.robot");
        try {
            restoreProperty("bisq.uiTest", optIn);
            // Any attempt to construct a TestFX robot must fail, even before a test instance exists.
            System.setProperty("testfx.robot", "forbidden-in-normal-tests");
            var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass("bisq.desktop.main.content.chat.message_container.ChatMessageContainerViewTest"),
                            selectClass("bisq.desktop.main.content.mu_sig.offer.draft.amount_components.text_input.MuSigAmountTextInputControllerTest"))
                    .build(), listener);

            var summary = listener.getSummary();
            assertTrue(summary.getTestsFoundCount() > 0);
            assertEquals(summary.getTestsFoundCount(), summary.getTestsSkippedCount());
            assertEquals(0, summary.getTestsStartedCount());
            assertEquals(0, summary.getTotalFailureCount());
        } finally {
            restoreProperty("bisq.uiTest", previousOptIn);
            restoreProperty("testfx.robot", previousRobot);
        }
    }

    private static void restoreProperty(String name, @Nullable String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
