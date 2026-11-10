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

package bisq.wallet;

import bisq.common.proto.UnresolvableProtobufException;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletStoreTest {
    @Test
    void aNewStoreHasNoWallet() {
        WalletStore store = new WalletStore();

        assertFalse(store.isWalletExists());
        assertFalse(store.getClone().isWalletExists());
    }

    @Test
    void setWalletExistsReportsTheFirstChangeOnly() {
        WalletStore store = new WalletStore();

        assertTrue(store.setWalletExists());
        assertFalse(store.setWalletExists());
        assertTrue(store.isWalletExists());
    }

    @Test
    void theFlagSurvivesTheProtoRoundTripAndApplyPersisted() throws UnresolvableProtobufException {
        WalletStore store = new WalletStore();
        store.setWalletExists();

        WalletStore restored = (WalletStore) store.getResolver().fromAny(Any.pack(store.toProto(false)));
        WalletStore target = new WalletStore();
        target.applyPersisted(restored);

        assertTrue(restored.isWalletExists());
        assertTrue(target.isWalletExists());
    }
}
