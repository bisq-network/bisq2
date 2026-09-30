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

import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesktopAutomationMetadataTest {
    @Test
    void storesTrimmedScopeAndAutomationId() {
        Pane pane = new Pane();

        DesktopAutomationMetadata.setScope(pane, " chat-message-container ");
        DesktopAutomationMetadata.setId(pane, " send ");

        assertThat(DesktopAutomationMetadata.getScope(pane)).contains("chat-message-container");
        assertThat(DesktopAutomationMetadata.getId(pane)).contains("send");
    }

    @Test
    void rejectsBlankOrSlashDelimitedValues() {
        Pane pane = new Pane();

        assertThatThrownBy(() -> DesktopAutomationMetadata.setScope(pane, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
        assertThatThrownBy(() -> DesktopAutomationMetadata.setId(pane, "send/button"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("automationId");
    }
}
