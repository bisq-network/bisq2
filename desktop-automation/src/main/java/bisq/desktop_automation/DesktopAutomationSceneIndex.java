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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DesktopAutomationSceneIndex {
    private final Map<DesktopAutomationSelector, Node> nodesBySelector;
    private final List<DesktopAutomationNodeDescriptor> nodes;
    private final List<DesktopAutomationValidationIssue> validationIssues;

    private DesktopAutomationSceneIndex(Map<DesktopAutomationSelector, Node> nodesBySelector,
                                        List<DesktopAutomationNodeDescriptor> nodes,
                                        List<DesktopAutomationValidationIssue> validationIssues) {
        this.nodesBySelector = nodesBySelector;
        this.nodes = List.copyOf(nodes);
        this.validationIssues = List.copyOf(validationIssues);
    }

    static DesktopAutomationSceneIndex create(@Nullable Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return new DesktopAutomationSceneIndex(Map.of(), List.of(), List.of());
        }
        return createFromMultipleScenes(List.of(scene));
    }

    static DesktopAutomationSceneIndex createFromMultipleScenes(List<Scene> scenes) {
        Map<DesktopAutomationSelector, Node> nodesBySelector = new LinkedHashMap<>();
        List<DesktopAutomationNodeDescriptor> nodes = new ArrayList<>();
        List<DesktopAutomationValidationIssue> validationIssues = new ArrayList<>();
        Map<String, String> scopeRootsByName = new LinkedHashMap<>();
        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            Scene scene = scenes.get(sceneIndex);
            if (scene == null || scene.getRoot() == null) {
                continue;
            }
            String rootPath = "Scene[" + sceneIndex + "]/" + scene.getRoot().getClass().getSimpleName();
            collect(scene.getRoot(), null, rootPath,
                    nodesBySelector, nodes, validationIssues, scopeRootsByName);
        }
        return new DesktopAutomationSceneIndex(nodesBySelector, nodes, validationIssues);
    }

    boolean isValid() {
        return validationIssues.isEmpty();
    }

    @Nullable
    Node find(DesktopAutomationSelector selector) {
        return nodesBySelector.get(selector);
    }

    List<DesktopAutomationNodeDescriptor> nodes() {
        return nodes;
    }

    List<DesktopAutomationValidationIssue> validationIssues() {
        return validationIssues;
    }

    private static void collect(Node node,
                                @Nullable String inheritedScope,
                                String path,
                                Map<DesktopAutomationSelector, Node> nodesBySelector,
                                List<DesktopAutomationNodeDescriptor> nodes,
                                List<DesktopAutomationValidationIssue> validationIssues,
                                Map<String, String> scopeRootsByName) {
        String currentScope = inheritedScope;
        String declaredScope = DesktopAutomationMetadata.getScope(node).orElse(null);
        if (declaredScope != null) {
            String previousPath = scopeRootsByName.putIfAbsent(declaredScope, path);
            if (previousPath != null) {
                validationIssues.add(new DesktopAutomationValidationIssue(
                        "duplicate_scope",
                        "Duplicate automation scope '" + declaredScope + "' at " + previousPath + " and " + path));
            }
            currentScope = declaredScope;
        }

        String automationId = DesktopAutomationMetadata.getId(node).orElse(null);
        String selectorString = null;
        if (automationId != null) {
            if (currentScope == null) {
                validationIssues.add(new DesktopAutomationValidationIssue(
                        "missing_scope",
                        "Node at " + path + " declares automation id '" + automationId + "' without an enclosing scope"));
            } else {
                DesktopAutomationSelector selector = new DesktopAutomationSelector(currentScope, automationId);
                Node previousNode = nodesBySelector.putIfAbsent(selector, node);
                selectorString = selector.asString();
                if (previousNode != null) {
                    validationIssues.add(new DesktopAutomationValidationIssue(
                            "duplicate_selector",
                            "Duplicate automation selector '" + selectorString + "' detected at " + path));
                }
            }

            nodes.add(new DesktopAutomationNodeDescriptor(
                    selectorString,
                    currentScope,
                    automationId,
                    emptyToNull(node.getId()),
                    node.getClass().getSimpleName(),
                    DesktopAutomationServer.extractNodeText(node),
                    node.isVisible() && node.getScene() != null,
                    !node.isDisabled(),
                    node.isFocused(),
                    path
            ));
        }

        if (node instanceof Parent parent) {
            List<Node> children = parent.getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                String childPath = path + "/" + child.getClass().getSimpleName() + "[" + i + "]";
                collect(child, currentScope, childPath, nodesBySelector, nodes, validationIssues, scopeRootsByName);
            }
        }
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
