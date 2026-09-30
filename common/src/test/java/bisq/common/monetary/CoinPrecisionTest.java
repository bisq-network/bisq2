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

package bisq.common.monetary;

import bisq.common.asset.CryptoAsset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoinPrecisionTest {

    @Test
    void precisionComesFromTheAssetRepository() {
        // Behaviour-preserving: the values previously hardcoded in Coin.derivePrecision now come
        // from CryptoAssetRepository via the asset's own precision.
        assertEquals(12, Coin.fromFaceValue(1.0, "XMR").getPrecision());
        assertEquals(2, Coin.fromFaceValue(1.0, "BSQ").getPrecision());
        assertEquals(8, Coin.fromFaceValue(1.0, "BTC").getPrecision());
        // A code not listed in the repository (custom crypto) falls back to 8.
        assertEquals(8, Coin.fromFaceValue(1.0, "FOO").getPrecision());
    }

    @Test
    void lowPrecisionNeverExceedsPrecision() {
        // Display low precision is min(precision, 4); this also covers the former BSQ special case.
        assertEquals(2, Coin.fromFaceValue(1.0, "BSQ").getLowPrecision());
        assertEquals(4, Coin.fromFaceValue(1.0, "BTC").getLowPrecision());
        assertEquals(4, Coin.fromFaceValue(1.0, "XMR").getLowPrecision());
    }

    @Test
    void subEightDecimalAssetKeepsItsPrecision() {
        // The point of the change: an asset with fewer than 8 decimals reports its real precision,
        // so its trade amount stays sendable (a future 6-decimal stablecoin cannot silently use 8).
        assertEquals(6, new CryptoAsset("USDC", "USDC", 6).getPrecision());
    }

    @Test
    void customAssetDefaultsToEight() {
        assertEquals(8, new CryptoAsset("FOO").getPrecision());
    }

    @Test
    void precisionOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CryptoAsset("FOO", "Foo", 13));
        assertThrows(IllegalArgumentException.class, () -> new CryptoAsset("FOO", "Foo", -1));
    }
}
