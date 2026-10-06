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

package bisq.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mediators across the versions (mediators-across-versions; mediation-cross-version of the release test plan). The
 * authorized data publisher acts as a root oracle node of this checkout with a dev key. It publishes its own data and
 * the roles of two mediators, as the oracle node of this checkout publishes them for registrations: the mediator of the
 * released version with the legacy registration protocol, and the mediator of this checkout with the protocol of the
 * proposal key.
 * <ul>
 *     <li>A trader of the released version requests mediation, and the mediator of this checkout answers both traders.
 *     The mediator gets the chat of the trade, also a message after the request.</li>
 *     <li>With both mediators, makers and takers of both versions select the same mediator for each take. A maker
 *     rejects a take whose mediator differs from its own selection, so each completed take shows this.</li>
 *     <li>The oracle removes the role of the mediator of this checkout. A trader of this checkout requests mediation
 *     with a long chat, and the mediator of the released version answers both traders and gets the chat.</li>
 * </ul>
 * Closing and opening a case again need the screens of the mediator, so they stay a manual test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class MediationAcrossVersionsTest {
    private static final String ORACLE = "oracle";
    private static final int LEGACY_REGISTRATION = 1;
    private static final int PROPOSAL_KEY_REGISTRATION = 2;
    // Nodes in dev mode log each new entry of their store of roles
    private static final String ROLE_ADDED = "Method: add success; map entry: AuthorizedBondedRole";
    private static final String ROLE_REMOVED = "Method: remove success; map entry: AuthorizedBondedRole";
    // A node logs the change of its store before it notifies the service of the roles
    private static final Duration ROLE_APPLIED = Duration.ofSeconds(3);
    // The texts of the messages which a trader adds to the chat when the answer of the mediator arrives. The API of
    // v2.1.13 does not put the channel in mediation when it sends the request, unlike its desktop and this version, so
    // a requester of v2.1.13 shows the text for the peer.
    private static final String ANSWER_TO_REQUESTER = "authorizedRole.mediator.message.toRequester";
    private static final String ANSWER_TO_PEER = "authorizedRole.mediator.message.toNonRequester";
    // Each take selects one of two mediators by the hash of the IDs. The chance that 12 takes miss one is below 0.1 %.
    private static final int MAX_TAKES_FOR_BOTH_MEDIATORS = 12;

    private MixedVersionNetwork network;
    private ApiClient mediatorOld;
    private ApiClient mediatorNew;
    private String mediatorOldId;
    private String mediatorNewId;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("mediation-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void mediatorsOfBothVersionsMediateTradesBetweenTheVersions() throws Exception {
        List<Integer> seeds = List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort());
        ApiClient oracle = network.startAuthorizedDataPublisher(ORACLE);
        mediatorOld = network.startExtraApiNode("mediator-old", false, seeds);
        mediatorNew = network.startExtraApiNode("mediator-new", true, seeds);
        mediatorOldId = mediatorOld.userProfileId();
        mediatorNewId = mediatorNew.userProfileId();

        // The root oracle publishes its own data first, then the role of the mediator of this version
        network.runPublisherCommand(ORACLE, "oracle");
        awaitRolesOfTradersAndMediators(1);
        MixedVersionNetwork.await("the oracle knows the profile of the new mediator", Trades.STEP_TIMEOUT,
                () -> oracle.knowsUserProfile(mediatorNewId));
        network.runPublisherCommand(ORACLE, "mediator " + mediatorNewId + " " + PROPOSAL_KEY_REGISTRATION);
        awaitRolesOfTradersAndMediators(2);

        // A requester of the released version and the mediator of this version
        String firstTrade = takeAndCompareMediators(network.oldApi, network.newApi, "SELL");
        assertEquals(mediatorNewId, mediatorOf(network.oldApi, firstTrade), "mediator of the first trade");
        mediate(network.oldApi, ANSWER_TO_PEER, network.newApi, firstTrade, mediatorNew, "the new mediator",
                List.of("A message of the old trader before the request"));

        // With both mediators, both versions select the same one for each take
        MixedVersionNetwork.await("the oracle knows the profile of the old mediator", Trades.STEP_TIMEOUT,
                () -> oracle.knowsUserProfile(mediatorOldId));
        network.runPublisherCommand(ORACLE, "mediator " + mediatorOldId + " " + LEGACY_REGISTRATION);
        awaitRolesOfTradersAndMediators(3);
        Set<String> selectedMediators = new HashSet<>();
        List<ApiClient[]> makerAndTaker = List.of(new ApiClient[]{network.oldApi, network.newApi},
                new ApiClient[]{network.newApi, network.oldApi});
        for (int take = 0; take < MAX_TAKES_FOR_BOTH_MEDIATORS && selectedMediators.size() < 2; take++) {
            ApiClient[] pair = makerAndTaker.get(take % 2);
            String direction = (take / 2) % 2 == 0 ? "SELL" : "BUY";
            String tradeId = takeAndCompareMediators(pair[0], pair[1], direction);
            selectedMediators.add(mediatorOf(pair[0], tradeId));
        }
        assertEquals(Set.of(mediatorOldId, mediatorNewId), selectedMediators,
                "The takes must select each mediator at least once");

        // The oracle removes the role of the mediator of this version. A requester of this version with a long chat
        // and the mediator of the released version.
        network.runPublisherCommand(ORACLE, "remove-mediator " + mediatorNewId);
        for (String nodeName : List.of(network.oldNode.getName(), network.newNode.getName())) {
            awaitLogLines(nodeName, ROLE_REMOVED, 1, nodeName + " removes the role of the new mediator");
        }
        Thread.sleep(ROLE_APPLIED.toMillis());
        String lastTrade = takeAndCompareMediators(network.newApi, network.oldApi, "SELL");
        assertEquals(mediatorOldId, mediatorOf(network.newApi, lastTrade), "mediator after the removal");
        mediate(network.newApi, ANSWER_TO_REQUESTER, network.oldApi, lastTrade, mediatorOld, "the old mediator",
                List.of("A long message of the new trader: " + "a".repeat(3_000),
                        "A second long message of the new trader: " + "b".repeat(3_000)));
    }

    // Each node of a trader or a mediator has stored the given number of roles: the oracle and the mediators
    private void awaitRolesOfTradersAndMediators(int roles) throws InterruptedException {
        for (String nodeName : List.of(network.oldNode.getName(), network.newNode.getName(), "mediator-old",
                "mediator-new")) {
            awaitLogLines(nodeName, ROLE_ADDED, roles, nodeName + " stores " + roles + " roles");
        }
        Thread.sleep(ROLE_APPLIED.toMillis());
    }

    private void awaitLogLines(String nodeName, String text, int count, String description)
            throws InterruptedException {
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT,
                () -> network.countLogLines(nodeName, text) >= count);
    }

    // The maker accepts the take only if it selected the same mediator as the taker
    private String takeAndCompareMediators(ApiClient maker, ApiClient taker, String direction) throws Exception {
        String offerId = Trades.createOffer(maker, direction);
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer");
        String tradeId = taker.takeOffer(offerId, Trades.baseAmount(network.priceServer), Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(taker, tradeId, "the maker accepts the take", state -> state.contains("RECEIVED"));
        Trades.awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        assertEquals(mediatorOf(taker, tradeId), mediatorOf(maker, tradeId), "mediator of maker and taker");
        return tradeId;
    }

    private static String mediatorOf(ApiClient api, String tradeId) {
        return Trades.findTrade(api, tradeId).orElseThrow()
                .path("mediatorUserProfile").path("networkId").path("pubKey").path("id").asText();
    }

    // The requester sends the messages, then requests mediation. The mediator answers both traders, gets the messages,
    // and gets a message of the requester after the request.
    private void mediate(ApiClient requester,
                         String answerToRequester,
                         ApiClient peer,
                         String tradeId,
                         ApiClient mediator,
                         String mediatorName,
                         List<String> messagesBefore) throws InterruptedException {
        String channelId = TradeChats.channelId(requester, tradeId);
        for (String text : messagesBefore) {
            requester.sendTradeChatMessage(channelId, text, null);
            TradeChats.awaitMessage(peer, channelId, text, "the peer gets the message");
        }
        requester.requestMediation(tradeId);
        awaitMessageContaining(requester, channelId, answerToRequester, "the requester gets the answer of " +
                mediatorName);
        awaitMessageContaining(peer, channelId, ANSWER_TO_PEER, "the peer gets the answer of " + mediatorName);
        for (String text : messagesBefore) {
            TradeChats.awaitMessage(mediator, channelId, text, mediatorName + " gets the chat of the trade");
        }
        String textAfter = "A message of the requester to " + mediatorName;
        requester.sendTradeChatMessage(channelId, textAfter, null);
        TradeChats.awaitMessage(mediator, channelId, textAfter, mediatorName + " gets a message after the request");
        TradeChats.awaitMessage(peer, channelId, textAfter, "the peer gets the message after the request");
    }

    private static void awaitMessageContaining(ApiClient api, String channelId, String text, String description)
            throws InterruptedException {
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT, () -> TradeChats.messages(api, channelId).stream()
                .map(TradeChats::textOf)
                .anyMatch(messageText -> messageText != null && messageText.contains(text)));
    }
}
