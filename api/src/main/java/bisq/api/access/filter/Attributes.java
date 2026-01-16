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

package bisq.api.access.filter;

public final class Attributes {
    // Authentication
    public static final String IS_AUTHENTICATED = "bisq.authn.is_authenticated";
    public static final String SESSION_ID = "bisq.authn.session_id";

    // Authorization
    public static final String DEVICE_ID = "bisq.authz.device_id";
    public static final String PERMISSIONS = "bisq.authz.permissions";

    // Metadata
    public static final String USER_AGENT = "bisq.meta.user_agent";
    public static final String REMOTE_ADDRESS = "bisq.meta.remote_address";

}

