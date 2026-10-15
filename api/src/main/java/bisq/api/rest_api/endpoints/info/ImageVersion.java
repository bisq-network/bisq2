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

package bisq.api.rest_api.endpoints.info;

import java.util.Optional;
import java.util.function.UnaryOperator;

// Clients key caches of static node config on the reported version, so a distribution that can be
// updated in place under the same core version (docker image rebuilds) reports the per-image release
// in a separate field. version itself must stay the plain core version: deployed clients parse it
// strictly as MAJOR.MINOR.PATCH and refuse the connection otherwise.
public final class ImageVersion {
    private static final String ENV_VARIABLE = "BISQ_IMAGE_VERSION";

    private ImageVersion() {
    }

    public static Optional<String> resolve(UnaryOperator<String> env) {
        return Optional.ofNullable(env.apply(ENV_VARIABLE))
                .filter(imageVersion -> !imageVersion.isBlank())
                .map(String::strip);
    }
}
