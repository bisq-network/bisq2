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


package bisq.application;

import bisq.network.p2p.services.data.storage.MetaData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the ttl, priority and maxMapSize of every storable type, so that a change of one of them is a deliberate edit
 * in two places. The rows were taken from the declarations, so the test reports a change of a value, not a wrong
 * value.
 * <p>
 * ttl decides when the storage drops an entry (rule 10 of docs/specifications/network/storage-metadata.md), and
 * priority decides which entries an inventory response keeps when it hits its size limit (rule 4). Both apply to every
 * node which runs the build, and neither shows up in a failing test or a visible symptom when it changes.
 * <p>
 * StorableTypeMessageCostTest covers ttl and maxMapSize only where the storage cost factor still reacts to them. That
 * factor is {@code min(1, ttl / 100 days) + min(1, maxMapSize / 10 000)}, bounded to 1 and rounded to two decimals, so
 * it does not react at all for a type which already reaches the bound, for example AuthorizedAlertData,
 * AuthorizedBondedRole or UserProfile. It does not cover priority for any type.
 */
class StorableTypeMetaDataValuesTest {
    private record Values(long ttl, int priority, int maxMapSize) {
    }

    private static final Map<Class<?>, Values> EXPECTED = expected();

    @Test
    void everyStorableTypeHasAPinnedRow() throws Exception {
        List<Class<?>> storableTypes = StorableTypes.find();
        // The scan must see the types of all modules. If it finds only a few, the classpath scan does not work.
        assertTrue(storableTypes.size() > 50, "Found only " + storableTypes.size() + " storable types");

        List<String> errors = new ArrayList<>();
        for (Class<?> type : storableTypes) {
            if (!EXPECTED.containsKey(type)) {
                MetaData metaData = (MetaData) type.getDeclaredField("META_DATA").get(null);
                errors.add(type.getName() + " has no pinned row. Add map.put(" + type.getName() + ".class, " +
                        "new Values(" + metaData.getTtl() + "L, " + metaData.getPriority() + ", " +
                        metaData.getMaxMapSize() + "));");
            }
        }
        for (Class<?> type : EXPECTED.keySet()) {
            if (!storableTypes.contains(type)) {
                errors.add(type.getName() + " is pinned but is not a storable type anymore. Remove its row");
            }
        }
        assertEquals(List.of(), errors);
    }

    /**
     * If this test fails, check whether the change of the value was intended. ttl and priority are visible to the
     * other nodes: the entries of a node with a longer ttl stay in its inventory responses for longer, and priority
     * decides which entries a truncated response keeps. Update the row only for an intended change.
     */
    @Test
    void pinnedValuesAreUnchanged() throws Exception {
        List<String> errors = new ArrayList<>();
        for (Class<?> type : StorableTypes.find()) {
            Values expected = EXPECTED.get(type);
            if (expected == null) {
                // Reported by everyStorableTypeHasAPinnedRow
                continue;
            }
            MetaData metaData = (MetaData) type.getDeclaredField("META_DATA").get(null);
            Values actual = new Values(metaData.getTtl(), metaData.getPriority(), metaData.getMaxMapSize());
            if (!expected.equals(actual)) {
                errors.add(type.getName() + ": pinned " + expected + ", declared " + actual);
            }
        }
        assertEquals(List.of(), errors);
    }

    private static Map<Class<?>, Values> expected() {
        Map<Class<?>, Values> map = new LinkedHashMap<>();
        map.put(bisq.account.timestamp.AccountTimestamp.class, new Values(2592000000L, 0, 1000));
        map.put(bisq.account.timestamp.AuthorizeAccountTimestampV1Request.class, new Values(864000000L, 0, 100));
        map.put(bisq.account.timestamp.AuthorizeAccountTimestampV2Request.class, new Values(864000000L, 0, 100));
        map.put(bisq.account.timestamp.AuthorizedAccountTimestamp.class, new Values(1728000000L, 0, 1000));
        map.put(bisq.bonded_roles.bonded_role.AuthorizedBondedRole.class, new Values(8640000000L, 2, 100));
        map.put(bisq.bonded_roles.market_price.AuthorizedMarketPriceData.class, new Values(600000L, 0, 1000));
        map.put(bisq.bonded_roles.oracle.AuthorizedOracleNode.class, new Values(8640000000L, 2, 100));
        map.put(bisq.bonded_roles.registration.BondedRoleRegistrationRequest.class, new Values(864000000L, 0, 100));
        map.put(bisq.bonded_roles.release.ReleaseNotification.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.bonded_roles.security_manager.alert.AuthorizedAlertData.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.burningman.AuthorizedBurningmanListByBlock.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage.class, new Values(864000000L, -1, 10000));
        map.put(bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage.class, new Values(864000000L, 1, 100));
        map.put(bisq.chat.common.CommonPublicChatMessage.class, new Values(864000000L, -1, 10000));
        map.put(bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage.class, new Values(864000000L, 1, 100));
        map.put(bisq.chat.reactions.BisqEasyOfferbookMessageReaction.class, new Values(864000000L, -1, 10000));
        map.put(bisq.chat.reactions.BisqEasyOpenTradeMessageReaction.class, new Values(864000000L, 1, 100));
        map.put(bisq.chat.reactions.CommonPublicChatMessageReaction.class, new Values(864000000L, -1, 10000));
        map.put(bisq.chat.reactions.MuSigOpenTradeMessageReaction.class, new Values(864000000L, 1, 100));
        map.put(bisq.chat.reactions.TwoPartyPrivateChatMessageReaction.class, new Values(1296000000L, 0, 100));
        map.put(bisq.chat.two_party.TwoPartyPrivateChatMessage.class, new Values(1296000000L, 0, 100));
        map.put(bisq.network.p2p.services.confidential.ack.AckMessage.class, new Values(432000000L, -1, 100));
        map.put(bisq.offer.mu_sig.MuSigOfferMessage.class, new Values(172800000L, 0, 1000));
        map.put(bisq.support.arbitration.mu_sig.MuSigArbitrationRequest.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.arbitration.mu_sig.MuSigArbitrationStateChangeMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.dispute.mu_sig.MuSigDisputeCaseDataMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.mediation.bisq_easy.BisqEasyMediationRequest.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.mediation.bisq_easy.BisqEasyMediatorsResponse.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.mediation.mu_sig.MuSigMediationRequest.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.support.moderator.ReportToModeratorMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyCancelTradeMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmBtcSentMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyRejectTradeMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferResponse.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.CooperativeClosureMessage_G.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.PaymentInitiatedMessage_E.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SendAccountPayloadAndDepositTxMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SetupTradeMessage_A.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SetupTradeMessage_B.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SetupTradeMessage_C.class, new Values(864000000L, 1, 1000));
        map.put(bisq.trade.mu_sig.messages.network.SetupTradeMessage_D.class, new Values(864000000L, 1, 1000));
        map.put(bisq.user.banned.BannedUserProfileData.class, new Values(8640000000L, 1, 1000));
        map.put(bisq.user.profile.UserProfile.class, new Values(1296000000L, 0, 10000));
        map.put(bisq.user.reputation.data.AuthorizedAccountAgeData.class, new Values(2592000000L, 2, 1000));
        map.put(bisq.user.reputation.data.AuthorizedBondedReputationData.class, new Values(2592000000L, 1, 1000));
        map.put(bisq.user.reputation.data.AuthorizedProofOfBurnData.class, new Values(2592000000L, 1, 1000));
        map.put(bisq.user.reputation.data.AuthorizedSignedWitnessData.class, new Values(2592000000L, 1, 1000));
        map.put(bisq.user.reputation.data.AuthorizedTimestampData.class, new Values(2592000000L, 0, 1000));
        map.put(bisq.user.reputation.requests.AuthorizeAccountAgeRequest.class, new Values(864000000L, 0, 100));
        map.put(bisq.user.reputation.requests.AuthorizeSignedWitnessRequest.class, new Values(864000000L, 0, 100));
        map.put(bisq.user.reputation.requests.AuthorizeTimestampRequest.class, new Values(864000000L, 0, 1000));
        return Collections.unmodifiableMap(map);
    }
}
