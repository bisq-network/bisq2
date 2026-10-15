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

package bisq.api.access.pairing;

import bisq.api.access.identity.ClientProfile;

/**
 * The outcome of a pairing: the stored profile and, only here, the plaintext secret. The node keeps
 * a hash of it; this is the one place the secret exists on the way to the pairing response.
 */
public record NewPairing(ClientProfile clientProfile, String clientSecret) {
    /** Redacted: the generated form would put the secret into any log line that prints this. */
    @Override
    public String toString() {
        return "NewPairing[clientProfile=" + clientProfile + ", clientSecret=<redacted>]";
    }
}
