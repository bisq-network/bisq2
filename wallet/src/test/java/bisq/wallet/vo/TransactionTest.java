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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {
    private static final long BLOCK_TIME_SECONDS = 1_700_000_000L;

    @Test
    void unconfirmedTransactionHasNoDate() {
        Transaction transaction = Transaction.fromProto(bisq.wallet.protobuf.Transaction.newBuilder()
                .setTxId("unconfirmed")
                .build());

        assertTrue(transaction.findDate().isEmpty());
        assertEquals(0, transaction.getDate().getTime());
    }

    @Test
    void confirmedTransactionHasItsBlockDate() {
        Transaction transaction = Transaction.fromProto(bisq.wallet.protobuf.Transaction.newBuilder()
                .setTxId("confirmed")
                .setBlockHeight(800_001)
                .setDate(BLOCK_TIME_SECONDS)
                .setNumConfirmations(3)
                .build());

        Date blockDate = Date.from(Instant.ofEpochSecond(BLOCK_TIME_SECONDS));
        assertEquals(Optional.of(blockDate), transaction.findDate());
        assertEquals(blockDate, transaction.getDate());
    }

    @Test
    void dateIsKeptWithoutConfirmations() {
        Transaction fromDaemon = Transaction.fromProto(bisq.wallet.protobuf.Transaction.newBuilder()
                .setTxId("first seen")
                .setDate(BLOCK_TIME_SECONDS)
                .build());
        Transaction constructed = new Transaction("constructed", List.of(), List.of(), 0, 0,
                new Date(BLOCK_TIME_SECONDS * 1000), 0, 50_000L, true);

        assertTrue(fromDaemon.findDate().isPresent());
        assertTrue(constructed.findDate().isPresent());
    }
}
