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

import bisq.desktop.automation.DesktopAutomationMetadata;
import bisq.desktop.automation.DesktopAutomationSelector;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopAutomationSceneIndexTest {
    @BeforeAll
    static void initJavaFxToolkit() {
        new JFXPanel();
    }

    @Test
    void createsScopedSelectorsForAutomatableNodes() {
        VBox root = new VBox();
        DesktopAutomationMetadata.setScope(root, "chat-message-container");

        TextField input = new TextField("hello");
        DesktopAutomationMetadata.setId(input, "input");
        Button send = new Button("Send");
        DesktopAutomationMetadata.setId(send, "send");
        root.getChildren().addAll(input, new HBox(send));

        DesktopAutomationSceneIndex sceneIndex = DesktopAutomationSceneIndex.create(new Scene(root));

        assertThat(sceneIndex.isValid()).isTrue();
        assertThat(sceneIndex.validationIssues()).isEmpty();
        assertThat(sceneIndex.find(new DesktopAutomationSelector("chat-message-container", "input"))).isSameAs(input);
        assertThat(sceneIndex.find(new DesktopAutomationSelector("chat-message-container", "send"))).isSameAs(send);
        assertThat(sceneIndex.nodes()).extracting(DesktopAutomationNodeDescriptor::selector)
                .containsExactly("chat-message-container/input", "chat-message-container/send");
    }

    @Test
    void reportsDuplicateSelectorsWithinTheSameScope() {
        VBox root = new VBox();
        DesktopAutomationMetadata.setScope(root, "chat-message-container");

        TextField input = new TextField();
        DesktopAutomationMetadata.setId(input, "input");
        Button duplicateInput = new Button("Duplicate");
        DesktopAutomationMetadata.setId(duplicateInput, "input");
        root.getChildren().addAll(input, duplicateInput);

        DesktopAutomationSceneIndex sceneIndex = DesktopAutomationSceneIndex.create(new Scene(root));

        assertThat(sceneIndex.isValid()).isFalse();
        assertThat(sceneIndex.validationIssues())
                .extracting(DesktopAutomationValidationIssue::code)
                .contains("duplicate_selector");
    }

    @Test
    void reportsAutomationIdsWithoutScope() {
        VBox root = new VBox();
        TextField input = new TextField();
        DesktopAutomationMetadata.setId(input, "input");
        root.getChildren().add(input);

        DesktopAutomationSceneIndex sceneIndex = DesktopAutomationSceneIndex.create(new Scene(root));

        assertThat(sceneIndex.isValid()).isFalse();
        assertThat(sceneIndex.validationIssues())
                .extracting(DesktopAutomationValidationIssue::code)
                .contains("missing_scope");
    }

    @Test
    void reportsDuplicateScopesAcrossScenes() {
        VBox primaryRoot = new VBox();
        DesktopAutomationMetadata.setScope(primaryRoot, "chat-message-container");
        Button primaryButton = new Button("Primary");
        DesktopAutomationMetadata.setId(primaryButton, "send");
        primaryRoot.getChildren().add(primaryButton);

        VBox overlayRoot = new VBox();
        DesktopAutomationMetadata.setScope(overlayRoot, "chat-message-container");
        Button overlayButton = new Button("Overlay");
        DesktopAutomationMetadata.setId(overlayButton, "close");
        overlayRoot.getChildren().add(overlayButton);

        DesktopAutomationSceneIndex sceneIndex = DesktopAutomationSceneIndex.createFromMultipleScenes(List.of(
                new Scene(primaryRoot),
                new Scene(overlayRoot)
        ));

        assertThat(sceneIndex.isValid()).isFalse();
        assertThat(sceneIndex.validationIssues())
                .extracting(DesktopAutomationValidationIssue::code)
                .contains("duplicate_scope");
        assertThat(sceneIndex.validationIssues())
                .extracting(DesktopAutomationValidationIssue::message)
                .anySatisfy(message -> assertThat(message).contains("Scene[0]", "Scene[1]"));
    }
}
