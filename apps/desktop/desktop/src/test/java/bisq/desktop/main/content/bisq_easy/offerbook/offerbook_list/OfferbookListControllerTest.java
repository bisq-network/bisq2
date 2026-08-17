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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.MarketRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferbookListControllerTest {
    @Test
    void previouslyInvalidOfferIsProcessedWhenARescanFindsItValid() {
        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getId()).thenReturn("offer-message-id");
        when(message.getAuthorUserProfileId()).thenReturn("author-id");
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(messageService.isValid(message)).thenReturn(false, true);

        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MarketRepository.getUSDBitcoinMarket());
        channel.addChatMessage(message);
        Set<String> processedMessageIds = new HashSet<>();
        List<BisqEasyOfferbookMessage> processedMessages = new ArrayList<>();

        OfferbookListController.reprocessOffers(channel,
                processedMessageIds,
                messageService::isValid,
                processedMessages::add);
        assertThat(processedMessages).isEmpty();

        OfferbookListController.reprocessOffers(channel,
                processedMessageIds,
                messageService::isValid,
                processedMessages::add);

        assertThat(processedMessages).containsExactly(message);
    }
}
