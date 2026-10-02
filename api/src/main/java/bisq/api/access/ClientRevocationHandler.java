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

package bisq.api.access;

/**
 * Cleans up state a revoked client would otherwise keep, outside of its profile and sessions.
 * <p>
 * Removing the profile and the sessions is not sufficient on its own: WebSocket authentication
 * happens at the handshake only, so an established connection stays alive, and push notification
 * registrations are keyed by device, not by session. Handlers keep those concerns out of the
 * access layer while {@link ClientRevocationService#revokeClient(String)} stays the single place
 * that defines what revocation means.
 * <p>
 * Implementations must be safe to call for an unknown client ID, as revocation also runs for
 * clients whose profile was already removed.
 */
@FunctionalInterface
public interface ClientRevocationHandler {
    void onClientRevoked(String clientId);
}
