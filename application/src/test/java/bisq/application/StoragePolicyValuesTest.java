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
import bisq.network.p2p.services.data.storage.StoragePolicy;
import bisq.network.p2p.services.data.storage.StoragePolicyAware;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the storage properties every payload type resolves to. A ttl, a priority or a map cap is protocol level
 * policy: peers which disagree expire, prioritise or cap the same data differently, and nothing in a running
 * network reports the disagreement. Changing one is occasionally right, but it must be a decision rather than a
 * side effect, so the values are frozen here and a change has to be made twice.
 * <p>
 * The expected values were taken from the declarations as they stood before {@code @StoragePolicy} replaced them,
 * so this also shows that the move to the annotation preserved them.
 */
class StoragePolicyValuesTest {
    private record Expected(Class<?> type, long ttl, int priority, int maxMapSize) {
    }

    private static Expected entry(Class<?> type, long ttl, int priority, int maxMapSize) {
        return new Expected(type, ttl, priority, maxMapSize);
    }

    private static final List<Expected> EXPECTED = List.of(
            entry(bisq.account.timestamp.AccountTimestamp.class, 2592000000L, 0, 1000),
            entry(bisq.network.p2p.services.confidential.ack.AckMessage.class, 432000000L, -1, 100),
            entry(bisq.user.reputation.requests.AuthorizeAccountAgeRequest.class, 864000000L, 0, 100),
            entry(bisq.account.timestamp.AuthorizeAccountTimestampV1Request.class, 864000000L, 0, 100),
            entry(bisq.account.timestamp.AuthorizeAccountTimestampV2Request.class, 864000000L, 0, 100),
            entry(bisq.user.reputation.requests.AuthorizeSignedWitnessRequest.class, 864000000L, 0, 100),
            entry(bisq.user.reputation.requests.AuthorizeTimestampRequest.class, 864000000L, 0, 1000),
            entry(bisq.user.reputation.data.AuthorizedAccountAgeData.class, 2592000000L, 2, 1000),
            entry(bisq.account.timestamp.AuthorizedAccountTimestamp.class, 1728000000L, 0, 1000),
            entry(bisq.bonded_roles.security_manager.alert.AuthorizedAlertData.class, 8640000000L, 1, 1000),
            entry(bisq.user.reputation.data.AuthorizedBondedReputationData.class, 2592000000L, 1, 1000),
            entry(bisq.bonded_roles.bonded_role.AuthorizedBondedRole.class, 8640000000L, 2, 100),
            entry(bisq.burningman.AuthorizedBurningmanListByBlock.class, 8640000000L, 1, 1000),
            entry(bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData.class, 8640000000L, 1, 1000),
            entry(bisq.bonded_roles.market_price.AuthorizedMarketPriceData.class, 600000L, 0, 1000),
            entry(bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData.class, 8640000000L, 1, 1000),
            entry(bisq.bonded_roles.oracle.AuthorizedOracleNode.class, 8640000000L, 2, 100),
            entry(bisq.user.reputation.data.AuthorizedProofOfBurnData.class, 2592000000L, 1, 1000),
            entry(bisq.user.reputation.data.AuthorizedSignedWitnessData.class, 2592000000L, 1, 1000),
            entry(bisq.user.reputation.data.AuthorizedTimestampData.class, 2592000000L, 0, 1000),
            entry(bisq.user.banned.BannedUserProfileData.class, 8640000000L, 1, 1000),
            entry(bisq.support.mediation.bisq_easy.BisqEasyMediationRequest.class, 864000000L, 1, 1000),
            entry(bisq.support.mediation.bisq_easy.BisqEasyMediatorsResponse.class, 864000000L, 1, 1000),
            entry(bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage.class, 864000000L, -1, 10000),
            entry(bisq.chat.reactions.BisqEasyOfferbookMessageReaction.class, 864000000L, -1, 10000),
            entry(bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage.class, 864000000L, 1, 100),
            entry(bisq.chat.reactions.BisqEasyOpenTradeMessageReaction.class, 864000000L, 1, 100),
            entry(bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage.class, 864000000L, 1, 1000),
            entry(bisq.bonded_roles.registration.BondedRoleRegistrationRequest.class, 864000000L, 0, 100),
            entry(bisq.chat.common.CommonPublicChatMessage.class, 864000000L, -1, 10000),
            entry(bisq.chat.reactions.CommonPublicChatMessageReaction.class, 864000000L, -1, 10000),
            entry(bisq.support.arbitration.mu_sig.MuSigArbitrationRequest.class, 864000000L, 1, 1000),
            entry(bisq.support.arbitration.mu_sig.MuSigArbitrationStateChangeMessage.class, 864000000L, 1, 1000),
            entry(bisq.support.dispute.mu_sig.MuSigDisputeCaseDataMessage.class, 864000000L, 1, 1000),
            entry(bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest.class, 864000000L, 1, 1000),
            entry(bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse.class, 864000000L, 1, 1000),
            entry(bisq.support.mediation.mu_sig.MuSigMediationRequest.class, 864000000L, 1, 1000),
            entry(bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage.class, 864000000L, 1, 1000),
            entry(bisq.offer.mu_sig.MuSigOfferMessage.class, 172800000L, 0, 1000),
            entry(bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage.class, 864000000L, 1, 100),
            entry(bisq.chat.reactions.MuSigOpenTradeMessageReaction.class, 864000000L, 1, 100),
            entry(bisq.trade.mu_sig.messages.network.MuSigTradeMessage.class, 864000000L, 1, 1000),
            entry(bisq.bonded_roles.release.ReleaseNotification.class, 8640000000L, 1, 1000),
            entry(bisq.support.moderator.ReportToModeratorMessage.class, 864000000L, 1, 1000),
            entry(bisq.chat.two_party.TwoPartyPrivateChatMessage.class, 1296000000L, 0, 100),
            entry(bisq.chat.reactions.TwoPartyPrivateChatMessageReaction.class, 1296000000L, 0, 100),
            entry(bisq.user.profile.UserProfile.class, 1296000000L, 0, 10000));

    @Test
    void resolvedPropertiesAreUnchanged() {
        for (Expected expected : EXPECTED) {
            MetaData metaData = MetaData.from(expected.type());
            String type = expected.type().getSimpleName();
            assertEquals(expected.ttl(), metaData.getTtl(), type + " ttl");
            assertEquals(expected.priority(), metaData.getPriority(), type + " priority");
            assertEquals(expected.maxMapSize(), metaData.getMaxMapSize(), type + " maxMapSize");
            assertEquals(type, metaData.getClassName(), type + " className");
        }
    }

    /**
     * The trade messages inherit their policy from an abstract base. The numbers have to carry over while the
     * className does not, since that is what keeps each subclass in its own store file.
     */
    @Test
    void inheritingTypesTakeTheNumbersButNotTheStoreName() throws Exception {
        int checked = 0;
        for (Class<?> clazz : BisqClasses.bisqClasses()) {
            if (!StoragePolicyAware.class.isAssignableFrom(clazz)
                    || clazz.isInterface()
                    || Modifier.isAbstract(clazz.getModifiers())
                    || clazz.getMethod("getMetaData").getDeclaringClass() != StoragePolicyAware.class
                    || clazz.getDeclaredAnnotation(StoragePolicy.class) != null) {
                continue;
            }
            Class<?> declaring = clazz.getSuperclass();
            while (declaring != null && declaring.getDeclaredAnnotation(StoragePolicy.class) == null) {
                declaring = declaring.getSuperclass();
            }
            assertNotNull(declaring, clazz.getName() + " resolves a policy but no ancestor declares one");
            MetaData inherited = MetaData.from(clazz);
            MetaData base = MetaData.from(declaring);
            String type = clazz.getSimpleName();
            assertEquals(base.getTtl(), inherited.getTtl(), type + " ttl");
            assertEquals(base.getPriority(), inherited.getPriority(), type + " priority");
            assertEquals(base.getMaxMapSize(), inherited.getMaxMapSize(), type + " maxMapSize");
            assertEquals(type, inherited.getClassName(), type + " className");
            checked++;
        }
        // Only guards against the filter matching nothing. A count would fail on an ordinary change, adding a
        // subclass, and point the author at a number in a test rather than at what they did. That the scan
        // itself sees everything is asserted by everyDeclaringTypeIsPinned below.
        assertTrue(checked > 0, "No type inherits its policy, so this asserted nothing");
    }

    /**
     * Without this, a payload type added later would simply not be covered by the table above and nothing would
     * say so.
     */
    @Test
    void everyDeclaringTypeIsPinned() throws Exception {
        Set<String> declaring = BisqClasses.bisqClasses().stream()
                .filter(clazz -> clazz.getDeclaredAnnotation(StoragePolicy.class) != null)
                .map(Class::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> pinned = EXPECTED.stream()
                .map(expected -> expected.type().getName())
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(declaring, pinned,
                "A type declaring @StoragePolicy is not pinned here, or a pinned type no longer declares it");
    }
}
