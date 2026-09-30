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

import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public final class Transaction {
    private final String txId;
    private final List<TransactionInput> inputs;
    private final List<TransactionOutput> outputs;
    private final long lockTime; // Is unsigned int in Bitcoin, thus not fits into signed Integer in Java
    private final int blockHeight;
    private final Date date;
    private final int numConfirmations;
    private final long amount;
    private final boolean incoming;

    public static Transaction fromProto(bisq.wallet.protobuf.Transaction tx) throws UnresolvableProtobufException {
        return new Transaction(
                tx.getTxId(),
                ProtobufUtils.fromProtoStream(tx.getInputsList(), TransactionInput::fromProto).toList(),
                ProtobufUtils.fromProtoStream(tx.getOutputsList(), TransactionOutput::fromProto).toList(),
                tx.getLockTime(),
                tx.getBlockHeight(),
                Date.from(Instant.ofEpochSecond(tx.getDate())),
                tx.getNumConfirmations(),
                tx.getAmount(),
                tx.getIncoming()
        );
    }
}