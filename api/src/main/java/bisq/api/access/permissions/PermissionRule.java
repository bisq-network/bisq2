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

package bisq.api.access.permissions;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class PermissionRule {

    private final Pattern pathPattern;
    private final Optional<Set<String>> methods;
    private final Permission permission;

    public PermissionRule(String pathRegex,
                          Optional<Set<String>> methods,
                          Permission permission) {
        this.pathPattern = Pattern.compile(pathRegex);
        this.methods = methods;
        this.permission = permission;
    }

    public boolean matches(String path, String method) {
        return pathPattern.matcher(path).matches()
                && (methods.isEmpty() || methods.get().contains(method));
    }

    public Permission permission() {
        return permission;
    }
}
