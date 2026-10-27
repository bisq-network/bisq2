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

package bisq.api.dto.presentation.offerbook;

import bisq.api.dto.DtoMappings;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.json.JsonMapperProvider;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OfferItemPresentationDtoFactoryTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final String MAKER_ID = "maker-id";

    @BeforeAll
    static void setUp() {
        // The factory formats the offer date with a localized text
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void itemCarriesTheGivenSellersReputationVerdict() {
        assertThat(create(sellOffer(), false).hasSellerSufficientReputation()).isFalse();
        assertThat(create(sellOffer(), true).hasSellerSufficientReputation()).isTrue();
    }

    @Test
    void verdictIsSerializedUnderItsFieldName() throws Exception {
        String json = JsonMapperProvider.get().writeValueAsString(create(sellOffer(), false));

        assertThat(json).contains("\"hasSellerSufficientReputation\":false");
    }

    private OfferItemPresentationDto create(BisqEasyOffer offer, boolean hasSellerSufficientReputation) {
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(message.getAuthorUserProfileId()).thenReturn(MAKER_ID);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.findUserProfile(MAKER_ID)).thenReturn(Optional.of(mock(UserProfile.class)));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(MAKER_ID)).thenReturn(new ReputationScore(0, 0, 0));

        // Mapping a real network ID needs BouncyCastle, which the test classpath of this module cannot load.
        // Unstubbed, the mappings return null, which the item and its JSON accept.
        try (MockedStatic<DtoMappings.BisqEasyOfferMapping> ignoredOfferMapping = mockStatic(DtoMappings.BisqEasyOfferMapping.class);
             MockedStatic<DtoMappings.UserProfileMapping> ignoredProfileMapping = mockStatic(DtoMappings.UserProfileMapping.class)) {
            return OfferItemPresentationDtoFactory.create(userProfileService,
                    mock(UserIdentityService.class),
                    reputationService,
                    mock(MarketPriceService.class),
                    hasSellerSufficientReputation,
                    message).orElseThrow();
        }
    }

    private static BisqEasyOffer sellOffer() {
        // A fixed price and a fixed amount, so the formatters need no market price
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getMakersUserProfileId()).thenReturn(MAKER_ID);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(100_0000));
        when(offer.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(100_000, "USD")));
        return offer;
    }
}
