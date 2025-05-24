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

package bisq.trade.mu_sig.messages.network;

import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigReportErrorMessage extends MuSigTradeMessage {
    public static final int MAX_LENGTH_ERROR_MESSAGE = 500;
    public static final int MAX_LENGTH_STACKTRACE = 2000;

    private final String errorMessage;
    private final String stackTrace;

    public MuSigReportErrorMessage(String id,
                                   String tradeId,
                                   String protocolVersion,
                                   NetworkId sender,
                                   NetworkId receiver,
                                   String errorMessage,
                                   String stackTrace) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateText(errorMessage, MAX_LENGTH_ERROR_MESSAGE);
        NetworkDataValidation.validateText(stackTrace, MAX_LENGTH_STACKTRACE);
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigReportErrorMessage(toMuSigReportErrorMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigReportErrorMessage toMuSigReportErrorMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigReportErrorMessage.Builder builder = getMuSigReportErrorMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigReportErrorMessage.Builder getMuSigReportErrorMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigReportErrorMessage.newBuilder()
                .setErrorMessage(errorMessage)
                .setStackTrace(stackTrace);
    }

    public static MuSigReportErrorMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        var muSigReportErrorMessage = proto.getMuSigTradeMessage().getMuSigReportErrorMessage();
        return new MuSigReportErrorMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                muSigReportErrorMessage.getErrorMessage(),
                muSigReportErrorMessage.getStackTrace());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.3, 0.7);
    }
}