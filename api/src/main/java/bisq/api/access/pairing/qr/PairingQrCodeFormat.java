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

package bisq.api.access.pairing.qr;

public final class PairingQrCodeFormat {
    // ---- Versioning ----
    public static final byte VERSION = 1;

    // ---- Flags ----
    public static final byte FLAG_TLS_FINGERPRINT = 1; // 1 << 0;
    public static final byte FLAG_TOR_CLIENT_AUTH = 1 << 1;

    // ---- Limits ----
    public static final int MAX_PAIRING_CODE_BYTES = 4096;
    public static final int MAX_WS_URL_BYTES = 512;
    public static final int MAX_TLS_FINGERPRINT_BYTES = 128;
    public static final int MAX_TOR_SECRET_BYTES = 256;
}

