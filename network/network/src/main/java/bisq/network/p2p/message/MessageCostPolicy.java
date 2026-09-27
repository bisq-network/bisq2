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

package bisq.network.p2p.message;

import bisq.common.util.MathUtils;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.inventory.InventoryRequest;
import bisq.network.p2p.services.data.inventory.InventoryResponse;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeRequest;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeResponse;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.network.p2p.services.peer_group.keep_alive.Pong;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeRequest;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeResponse;
import bisq.network.p2p.services.reporting.ReportRequest;
import bisq.network.p2p.services.reporting.ReportResponse;
import com.google.common.annotations.VisibleForTesting;

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

/**
 * Defines the cost factor of the messages which peers send to each other. A peer authorizes a message only if the
 * sender paid a cost which grows with the cost factor of the message, for example the difficulty of a proof of work.
 * The policy does not depend on the method which pays the cost.
 * <p>
 * The sender and the receiver of a message compute its cost factor independently, and they often run different
 * versions. If they compute different values, the receiver can reject the message. So all values of this policy are
 * protocol constants: they do not depend on other definitions like MetaData, and an existing value must never change.
 * A new storable type adds its values. See docs/specifications/network/message-cost.md.
 */
public final class MessageCostPolicy {
    // Cost factor of a message type for which the policy has no rule, e.g. a MailboxMessage outside of the encrypted
    // envelope in which peers send it. Peers do not send such messages.
    private static final double UNEXPECTED_MESSAGE_COST_FACTOR = 1;

    private static final Map<Class<? extends EnvelopePayloadMessage>, Double> COST_FACTOR_BY_MESSAGE_TYPE =
            Map.ofEntries(
                    entry(ConnectionHandshake.Request.class, 0.05),
                    entry(ConnectionHandshake.Response.class, 0.05),
                    entry(CloseConnectionMessage.class, 0.05),
                    entry(Ping.class, 0.05),
                    entry(Pong.class, 0.05),
                    entry(NetworkLoadExchangeRequest.class, 0.05),
                    entry(NetworkLoadExchangeResponse.class, 0.05),
                    entry(PeerExchangeRequest.class, 0.1),
                    entry(PeerExchangeResponse.class, 0.1),
                    entry(InventoryRequest.class, 0.25),
                    entry(InventoryResponse.class, 0.1),
                    entry(ReportRequest.class, 0.5),
                    entry(ReportResponse.class, 0.2));

    // A ConfidentialMessage with a cipher text of this size has the cost factor 1
    private static final int CIPHER_TEXT_SIZE_OF_COST_FACTOR_1 = 20_000;

    // Storage cost factor of each storable type, by its simple class name. Each value is the storage cost factor which
    // versions up to 2.1.13 derive from the MetaData of the type, rounded to two decimal places.
    // StorableTypeMessageCostTest cross-checks the values with the current MetaData. A later change of the MetaData
    // does not change them.
    private static final Map<String, Double> STORAGE_COST_FACTOR_BY_CLASS_NAME = Map.ofEntries(
            // Authenticated data
            entry("AccountTimestamp", 0.4),
            entry("AuthorizedAccountAgeData", 0.4),
            entry("AuthorizedAccountTimestamp", 0.3),
            entry("AuthorizedAlertData", 1.0),
            entry("AuthorizedBondedReputationData", 0.4),
            entry("AuthorizedBondedRole", 1.0),
            entry("AuthorizedBurningmanListByBlock", 1.0),
            entry("AuthorizedDifficultyAdjustmentData", 1.0),
            entry("AuthorizedMarketPriceData", 0.1),
            entry("AuthorizedMinRequiredReputationScoreData", 1.0),
            entry("AuthorizedOracleNode", 1.0),
            entry("AuthorizedProofOfBurnData", 0.4),
            entry("AuthorizedSignedWitnessData", 0.4),
            entry("AuthorizedTimestampData", 0.4),
            entry("BannedUserProfileData", 1.0),
            entry("BisqEasyOfferbookMessage", 1.0),
            entry("BisqEasyOfferbookMessageReaction", 1.0),
            entry("CommonPublicChatMessage", 1.0),
            entry("CommonPublicChatMessageReaction", 1.0),
            entry("MuSigOfferMessage", 0.12),
            entry("ReleaseNotification", 1.0),
            entry("UserProfile", 1.0),

            // Mailbox messages
            entry("AckMessage", 0.06),
            entry("AuthorizeAccountAgeRequest", 0.11),
            entry("AuthorizeAccountTimestampV1Request", 0.11),
            entry("AuthorizeAccountTimestampV2Request", 0.11),
            entry("AuthorizeSignedWitnessRequest", 0.11),
            entry("AuthorizeTimestampRequest", 0.2),
            entry("BisqEasyAccountDataMessage", 0.2),
            entry("BisqEasyBtcAddressMessage", 0.2),
            entry("BisqEasyCancelTradeMessage", 0.2),
            entry("BisqEasyConfirmBtcSentMessage", 0.2),
            entry("BisqEasyConfirmFiatReceiptMessage", 0.2),
            entry("BisqEasyConfirmFiatSentMessage", 0.2),
            entry("BisqEasyMediationRequest", 0.2),
            entry("BisqEasyMediatorsResponse", 0.2),
            entry("BisqEasyOpenTradeMessage", 0.11),
            entry("BisqEasyOpenTradeMessageReaction", 0.11),
            entry("BisqEasyRejectTradeMessage", 0.2),
            entry("BisqEasyReportErrorMessage", 0.2),
            entry("BisqEasyTakeOfferRequest", 0.2),
            entry("BisqEasyTakeOfferResponse", 0.2),
            entry("BondedRoleRegistrationRequest", 0.11),
            entry("CooperativeClosureMessage_G", 0.2),
            entry("MuSigMediationRequest", 0.2),
            entry("MuSigMediationResultAcceptanceMessage", 0.2),
            entry("MuSigMediationStateChangeMessage", 0.2),
            entry("MuSigMediatorsResponse", 0.2),
            entry("MuSigOpenTradeMessage", 0.11),
            entry("MuSigOpenTradeMessageReaction", 0.11),
            entry("MuSigPaymentDetailsRequest", 0.2),
            entry("MuSigPaymentDetailsResponse", 0.2),
            entry("MuSigReportErrorMessage", 0.2),
            entry("PaymentInitiatedMessage_E", 0.2),
            entry("PaymentReceivedMessage_F", 0.2),
            entry("ReportToModeratorMessage", 0.2),
            entry("SendAccountPayloadAndDepositTxMessage", 0.2),
            entry("SendAccountPayloadMessage", 0.2),
            entry("SetupTradeMessage_A", 0.2),
            entry("SetupTradeMessage_B", 0.2),
            entry("SetupTradeMessage_C", 0.2),
            entry("SetupTradeMessage_D", 0.2),
            entry("TwoPartyPrivateChatMessage", 0.16),
            entry("TwoPartyPrivateChatMessageReaction", 0.16));

    // Data cost factor of each type of authenticated or append-only data, by its simple class name
    private static final Map<String, Double> DATA_COST_FACTOR_BY_CLASS_NAME = Map.ofEntries(
            entry("AccountTimestamp", 0.2),
            entry("AuthorizedAccountAgeData", 0.5),
            entry("AuthorizedAccountTimestamp", 0.2),
            entry("AuthorizedAlertData", 0.5),
            entry("AuthorizedBondedReputationData", 0.5),
            entry("AuthorizedBondedRole", 0.5),
            entry("AuthorizedBurningmanListByBlock", 0.5),
            entry("AuthorizedDifficultyAdjustmentData", 0.5),
            entry("AuthorizedMarketPriceData", 0.5),
            entry("AuthorizedMinRequiredReputationScoreData", 0.5),
            entry("AuthorizedOracleNode", 0.5),
            entry("AuthorizedProofOfBurnData", 0.5),
            entry("AuthorizedSignedWitnessData", 0.5),
            entry("AuthorizedTimestampData", 0.5),
            entry("BannedUserProfileData", 0.5),
            entry("BisqEasyOfferbookMessage", 0.3),
            entry("BisqEasyOfferbookMessageReaction", 0.5),
            entry("CommonPublicChatMessage", 0.3),
            entry("CommonPublicChatMessageReaction", 0.5),
            entry("MuSigOfferMessage", 0.3),
            entry("ReleaseNotification", 0.5),
            entry("UserProfile", 0.3));

    private MessageCostPolicy() {
    }

    /**
     * The cost factor of a message which a peer sends to another peer over a connection.
     */
    public static double getCostFactor(EnvelopePayloadMessage message) {
        Double costFactorOfType = COST_FACTOR_BY_MESSAGE_TYPE.get(message.getClass());
        if (costFactorOfType != null) {
            return costFactorOfType;
        }
        return switch (message) {
            case ConfidentialMessage confidentialMessage -> getConfidentialMessageCostFactor(confidentialMessage);
            case AddAuthenticatedDataRequest request -> {
                String className = request.getAuthenticatedSequentialData().getDistributedData().getClassName();
                yield MathUtils.bounded(0.25, 0.75, getStorageCostFactor(className) + getDataCostFactor(className));
            }
            case AddAppendOnlyDataRequest request ->
                    MathUtils.bounded(0.5, 1, getDataCostFactor(request.getAppendOnlyData().getClassName()));
            case AddMailboxRequest request -> {
                MailboxData mailboxData = request.getMailboxSequentialData().getMailboxData();
                double storageCostFactor = getStorageCostFactor(mailboxData.getClassName());
                double dataCostFactor = getConfidentialMessageCostFactor(mailboxData.getConfidentialMessage());
                yield MathUtils.bounded(0.25, 1, storageCostFactor + dataCostFactor);
            }
            case RemoveMailboxRequest request -> getRemoveOrRefreshCostFactor(request.getClassName());
            case RemoveAuthenticatedDataRequest request -> getRemoveOrRefreshCostFactor(request.getClassName());
            case RefreshAuthenticatedDataRequest request -> getRemoveOrRefreshCostFactor(request.getClassName());
            default -> UNEXPECTED_MESSAGE_COST_FACTOR;
        };
    }

    /**
     * @return the storage cost factor of the storable type, or empty if the policy has no value for the class name.
     */
    @VisibleForTesting
    public static Optional<Double> findStorageCostFactor(String className) {
        return Optional.ofNullable(STORAGE_COST_FACTOR_BY_CLASS_NAME.get(className));
    }

    /**
     * @return the data cost factor of the type of authenticated or append-only data, or empty if the policy has no
     * value for the class name.
     */
    @VisibleForTesting
    public static Optional<Double> findDataCostFactor(String className) {
        return Optional.ofNullable(DATA_COST_FACTOR_BY_CLASS_NAME.get(className));
    }

    private static double getConfidentialMessageCostFactor(ConfidentialMessage confidentialMessage) {
        int cipherTextSize = confidentialMessage.getConfidentialData().getCipherText().length;
        return MathUtils.bounded(0.1, 1, cipherTextSize / (double) CIPHER_TEXT_SIZE_OF_COST_FACTOR_1);
    }

    // The receiver of a remove or refresh request, or of mailbox data, cannot read the stored object. The class name
    // is the only input from the message.
    private static double getRemoveOrRefreshCostFactor(String className) {
        return MathUtils.bounded(0.1, 0.3, getStorageCostFactor(className));
    }

    // The authorization needs the cost factor of a request before StorageService checks the class name. A type which
    // the policy does not know (e.g. added in a later version) is not stored, so it adds nothing to the cost.
    // Otherwise the authorization could reject messages of newer nodes.
    private static double getStorageCostFactor(String className) {
        return STORAGE_COST_FACTOR_BY_CLASS_NAME.getOrDefault(className, 0d);
    }

    private static double getDataCostFactor(String className) {
        return DATA_COST_FACTOR_BY_CLASS_NAME.getOrDefault(className, 0d);
    }
}
