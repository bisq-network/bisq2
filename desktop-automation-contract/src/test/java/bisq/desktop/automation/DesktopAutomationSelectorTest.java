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

package bisq.desktop.automation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesktopAutomationSelectorTest {
    @Test
    void parseAcceptsScopeAndAutomationId() {
        DesktopAutomationSelector selector = DesktopAutomationSelector.parse("chat-message-container/send");

        assertThat(selector.scope()).isEqualTo("chat-message-container");
        assertThat(selector.automationId()).isEqualTo("send");
        assertThat(selector.asString()).isEqualTo("chat-message-container/send");
    }

    @Test
    void parseRejectsMalformedSelectors() {
        assertThatThrownBy(() -> DesktopAutomationSelector.parse("send"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DesktopAutomationSelector.parse("scope/"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DesktopAutomationSelector.parse("/id"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DesktopAutomationSelector.parse("a/b/c"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
