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

import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.common.util.StringUtils;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SellerReputationVerdict {
    // A failing check repeats on every call, so only the debug log carries the stack trace
    public static Optional<Boolean> find(BisqEasySellersReputationBasedTradeAmountService sellersReputationService,
                                         BisqEasyOffer offer) {
        try {
            return Optional.of(sellersReputationService.hasSellerSufficientReputation(offer));
        } catch (RuntimeException e) {
            // The maker chooses the offer ID
            String offerId = StringUtils.sanitizeForLog(offer.getId());
            log.warn("Could not check the seller reputation of offer {}: {}: {}",
                    offerId, e.getClass().getName(), StringUtils.sanitizeForLog(e.getMessage()));
            log.debug("Could not check the seller reputation of offer {}", offerId, e);
            return Optional.empty();
        }
    }
}
