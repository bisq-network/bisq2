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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Observable;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.FavouriteMarketsService;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class MarketChannelItemTest extends TestFxHeadlessSupport {
    @Test
    void numOffersFollowsOfferValidityChangesWithoutChatMessageEvents() {
        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MarketRepository.getUSDBitcoinMarket());
        Observable<Long> offerValidityRevision = new Observable<>(0L);
        boolean[] offerIsValid = {false};
        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        when(messageService.getOfferValidityRevision()).thenReturn(offerValidityRevision);
        when(messageService.getOffers(any(BisqEasyOfferbookChannel.class)))
                .thenAnswer(invocation -> offerIsValid[0] ? Stream.of(mock(BisqEasyOffer.class)) : Stream.empty());

        MarketChannelItem item = new MarketChannelItem(channel,
                mock(FavouriteMarketsService.class),
                mock(ChatNotificationService.class),
                mock(MarketPriceService.class),
                mock(UserProfileService.class),
                messageService);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(item.getNumOffers().get()).isZero();

        offerIsValid[0] = true;
        offerValidityRevision.set(1L);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(item.getNumOffers().get()).isEqualTo(1);

        item.dispose();
        offerIsValid[0] = false;
        offerValidityRevision.set(2L);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(item.getNumOffers().get()).isEqualTo(1);
    }
}
