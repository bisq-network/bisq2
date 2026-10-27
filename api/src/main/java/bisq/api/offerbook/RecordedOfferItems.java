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

package bisq.api.offerbook;

import bisq.api.dto.presentation.offerbook.OfferItemPresentationDto;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDtoFactory;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.StringUtils;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// The offers which the OFFERS subscribers hold: one item per shown message, with the item they received last. Only
// OffersWebSocketService writes it, under its offer update lock. The snapshot and the REST list read it.
@Slf4j
public class RecordedOfferItems {
    // The quote currency code is the channel's, so that a removal corrects the count which the addition changed
    public record RecordedOffer(String quoteCurrencyCode, boolean valid, OfferItemPresentationDto item) {
        public boolean hasSellerSufficientReputation() {
            return item.hasSellerSufficientReputation();
        }
    }

    // Keyed like the channel's message set, as authors choose the IDs
    private final Map<BisqEasyOfferbookMessage, RecordedOffer> offerByMessage = new ConcurrentHashMap<>();
    private final Map<String, Integer> countByQuoteCurrencyCode = new ConcurrentHashMap<>();
    // What the NUM_OFFERS subscribers hold. Published per market, so that a pass which changes many offers of a
    // market sends one event.
    @Getter
    private final ObservableHashMap<String, Integer> numOffersByQuoteCurrencyCode = new ObservableHashMap<>();
    // Set once the existing offers are recorded, so that the REST list reads the record instead of checking
    @Getter
    @Setter
    private volatile boolean tracking;
    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private final MarketPriceService marketPriceService;

    public RecordedOfferItems(UserService userService, BondedRolesService bondedRolesService) {
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
        marketPriceService = bondedRolesService.getMarketPriceService();
    }

    public Optional<RecordedOffer> find(BisqEasyOfferbookMessage message) {
        return Optional.ofNullable(offerByMessage.get(message));
    }

    // The item of a recorded offer for the OFFERS snapshot and the REST list, or empty when the offer is not recorded.
    // A fresh item carries the current amounts and reputation; the recorded one serves when none can be built.
    public Optional<OfferItemPresentationDto> snapshotItem(BisqEasyOfferbookMessage message) {
        return find(message).map(recorded -> {
            try {
                return OfferItemPresentationDtoFactory.create(userProfileService,
                        userIdentityService,
                        reputationService,
                        marketPriceService,
                        recorded.hasSellerSufficientReputation(),
                        message).orElse(recorded.item());
            } catch (RuntimeException e) {
                // The maker chooses the offer ID
                log.debug("Could not rebuild the item of offer {}",
                        StringUtils.sanitizeForLog(message.getBisqEasyOffer().orElseThrow().getId()), e);
                return recorded.item();
            }
        });
    }

    public void put(BisqEasyOfferbookMessage message, RecordedOffer recordedOffer) {
        if (offerByMessage.put(message, recordedOffer) == null) {
            countByQuoteCurrencyCode.merge(recordedOffer.quoteCurrencyCode(), 1, Integer::sum);
        }
    }

    public Optional<RecordedOffer> remove(BisqEasyOfferbookMessage message) {
        RecordedOffer removed = offerByMessage.remove(message);
        if (removed != null) {
            countByQuoteCurrencyCode.merge(removed.quoteCurrencyCode(), -1, Integer::sum);
        }
        return Optional.ofNullable(removed);
    }

    // Called under the lock which orders the writes, so that a pass cannot publish a count which an addition replaced
    public void publishCount(String quoteCurrencyCode) {
        Integer count = countByQuoteCurrencyCode.getOrDefault(quoteCurrencyCode, 0);
        if (!count.equals(numOffersByQuoteCurrencyCode.get(quoteCurrencyCode))) {
            numOffersByQuoteCurrencyCode.put(quoteCurrencyCode, count);
        }
    }

    @VisibleForTesting
    public int size() {
        return offerByMessage.size();
    }
}
