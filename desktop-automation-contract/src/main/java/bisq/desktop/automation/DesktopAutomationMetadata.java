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

import javafx.scene.Node;

import java.util.Objects;
import java.util.Optional;

public final class DesktopAutomationMetadata {
    public static final String SCOPE_KEY = "bisq.desktop.automation.scope";
    public static final String ID_KEY = "bisq.desktop.automation.id";

    private DesktopAutomationMetadata() {
    }

    public static void setScope(Node node, String scope) {
        node.getProperties().put(SCOPE_KEY, normalize(scope, "scope"));
    }

    public static void setId(Node node, String automationId) {
        node.getProperties().put(ID_KEY, normalize(automationId, "automationId"));
    }

    public static Optional<String> getScope(Node node) {
        return getStringProperty(node, SCOPE_KEY);
    }

    public static Optional<String> getId(Node node) {
        return getStringProperty(node, ID_KEY);
    }

    private static Optional<String> getStringProperty(Node node, String key) {
        Object value = node.getProperties().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(normalize(value.toString(), key));
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.contains("/")) {
            throw new IllegalArgumentException(fieldName + " must not contain '/'");
        }
        return normalized;
    }
}
