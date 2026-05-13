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

import java.util.Objects;

public record DesktopAutomationSelector(String scope, String automationId) {
    public DesktopAutomationSelector {
        scope = normalize(scope, "scope");
        automationId = normalize(automationId, "automationId");
    }

    public static DesktopAutomationSelector parse(String rawSelector) {
        String selector = normalizeRawSelector(rawSelector);
        int separatorIndex = selector.indexOf('/');
        if (separatorIndex <= 0 || separatorIndex == selector.length() - 1 || selector.indexOf('/', separatorIndex + 1) != -1) {
            throw new IllegalArgumentException("Invalid automation selector: " + selector + ". Expected format <scope>/<automationId>.");
        }
        return new DesktopAutomationSelector(selector.substring(0, separatorIndex), selector.substring(separatorIndex + 1));
    }

    public String asString() {
        return scope + "/" + automationId;
    }

    @Override
    public String toString() {
        return asString();
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

    private static String normalizeRawSelector(String rawSelector) {
        String normalized = Objects.requireNonNull(rawSelector, "selector must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("selector must not be blank");
        }
        return normalized;
    }
}
