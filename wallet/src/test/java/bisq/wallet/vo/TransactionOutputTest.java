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

package bisq.wallet.vo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionOutputTest {
    private static final String RECIPIENT = "bcrt1qpzm8wsr0zsc9kdr6hd3zmxqyng2cwgymtn3svg";
    private static final String CHANGE = "bcrt1pdu7d0lhu2fg5vmmnch2rzead4ruwaj3nz97l3t6tfd0elm5e39ds290a9r";

    @Test
    void outputPaidToAnOwnAddressIsMine() {
        Set<String> ownAddresses = Set.of(CHANGE);

        assertTrue(TransactionOutput.fromProto(output(CHANGE), ownAddresses).isMine());
        assertFalse(TransactionOutput.fromProto(output(RECIPIENT), ownAddresses).isMine());
    }

    @Test
    void outputWithoutAddressIsNeverMine() {
        assertFalse(TransactionOutput.fromProto(output(""), Set.of("", CHANGE)).isMine());
    }

    @Test
    void emptyAddressSetMarksNothing() {
        assertFalse(TransactionOutput.fromProto(output(CHANGE), Set.of()).isMine());
    }

    @Test
    void transactionMarksEachOfItsOutputs() {
        var proto = bisq.wallet.protobuf.Transaction.newBuilder()
                .setTxId("send")
                .addOutputs(output(CHANGE))
                .addOutputs(output(RECIPIENT))
                .build();

        List<TransactionOutput> outputs = Transaction.fromProto(proto, Set.of(CHANGE)).getOutputs();

        assertEquals(List.of(true, false), outputs.stream().map(TransactionOutput::isMine).toList());
    }

    private static bisq.wallet.protobuf.TransactionOutput output(String address) {
        return bisq.wallet.protobuf.TransactionOutput.newBuilder()
                .setValue(10_000_000L)
                .setAddress(address)
                .build();
    }
}
