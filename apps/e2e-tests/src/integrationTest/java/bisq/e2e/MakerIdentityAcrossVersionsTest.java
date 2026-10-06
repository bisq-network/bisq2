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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A taker of the released version takes an offer which a maker of this checkout published with its second identity,
 * while the first identity is selected. The maker finds the identity by the key of the take offer request, not by the
 * selected identity (docs/specifications/user/identity-by-key.md, take-offer-request-validation.md). The trade runs
 * under the second identity to the end.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class MakerIdentityAcrossVersionsTest {
    private static final String SECOND_NICK_NAME = "e2e-new-second";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("maker-identity-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void oldTakerTakesTheOfferOfTheSecondIdentityOfANewMaker() throws Exception {
        ApiClient maker = network.newApi;
        String firstProfileId = maker.userProfileId();
        maker.createUserIdentity(SECOND_NICK_NAME);
        String secondProfileId = maker.ownedUserProfiles().stream()
                .filter(profile -> profile.path("nickName").asText().equals(SECOND_NICK_NAME))
                .map(profile -> profile.path("networkId").path("pubKey").path("id").asText())
                .findFirst()
                .orElseThrow();

        // The second identity publishes the offer, then the user selects the first identity again
        maker.selectUserProfile(secondProfileId);
        String offerId = Trades.createOffer(maker, "SELL");
        maker.selectUserProfile(firstProfileId);
        assertEquals(firstProfileId, maker.userProfileId(), "The first identity must be selected again");

        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old taker sees the offer");
        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.oldApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        for (ApiClient api : List.of(maker, network.oldApi)) {
            JsonNode trade = Trades.findTrade(api, tradeId).orElseThrow();
            assertEquals(SECOND_NICK_NAME, trade.path("makerUserProfile").path("nickName").asText(),
                    "The trade must run under the second identity of the maker");
        }

        Trades.completeAfterTake(maker, network.oldApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }
}
