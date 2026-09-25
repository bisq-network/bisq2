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

package bisq.wallet.receive_address;

import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiveAddressStoreTest {
    private static final ReceiveAddressEntry ENTRY_WITH_NOTE =
            new ReceiveAddressEntry("bcrt1qpzm8wsr0zsc9kdr6hd3zmxqyng2cwgymtn3svg", 1790000000000L, Optional.of("savings"));
    private static final ReceiveAddressEntry ENTRY_WITHOUT_NOTE =
            new ReceiveAddressEntry("bcrt1p4gg0902yvu5qvtdke4qsk2x99up0s332r05532xcph78gvhywhhqe8vc8c", 1790000001000L, Optional.empty());

    // A store as persisted before the receive address messages moved out of wallet.proto.
    private static final String PERSISTED_STORE_HEX =
            "0a2e747970652e676f6f676c65617069732e636f6d2f77616c6c65742e526563656976654164647265737353746f7265" +
            "128b010a490a406263727431703467673039303279767535717674646b653471736b3278393975703073333332723035" +
            "353332786370683738677668797768687165387663386310d0bf83c598680a3e0a2c626372743171707a6d3877737230" +
            "7a7363396b6472366864337a6d7871796e6732637767796d746e337376671080b083c598681a07736176696e6773";

    @Test
    void persistedStoreKeepsItsTypeUrl() {
        assertEquals("type.googleapis.com/wallet.ReceiveAddressStore", createStore().toAny().getTypeUrl());
    }

    @Test
    void storePersistedBeforeTheProtoSplitIsStillReadable() throws Exception {
        Any persisted = Any.parseFrom(HexFormat.of().parseHex(PERSISTED_STORE_HEX));

        ReceiveAddressStore store = (ReceiveAddressStore) createStore().getResolver().fromAny(persisted);

        assertEquals(Set.of(ENTRY_WITH_NOTE, ENTRY_WITHOUT_NOTE), Set.copyOf(store.getReceiveAddressEntries()));
    }

    private static ReceiveAddressStore createStore() {
        ReceiveAddressStore store = new ReceiveAddressStore();
        store.addReceiveAddressEntry(ENTRY_WITH_NOTE);
        store.addReceiveAddressEntry(ENTRY_WITHOUT_NOTE);
        return store;
    }
}
