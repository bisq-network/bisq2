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

package bisq.common.monetary;

import bisq.common.asset.Asset;
import bisq.common.market.Market;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class TradeAmount {
    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;

    public TradeAmount(Monetary baseSideAmount, Monetary quoteSideAmount) {
        checkArgument(!baseSideAmount.getCode().equals(quoteSideAmount.getCode()),
                "baseSideAmount must not be the same code as the quote.quoteSideAmount");
        checkArgument(Asset.isBtc(baseSideAmount.getCode()) || Asset.isBtc(quoteSideAmount.getCode()),
                "Either baseSideAmount or quoteSideAmount must be BTC");
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
    }

    public TradeAmount clamp(TradeAmountRange limits) {
        return clamp(limits.getMin(), limits.getMax());
    }

    public TradeAmount clamp(TradeAmount min, TradeAmount max) {
        MonetaryRange baseSideLimits = new MonetaryRange(min.getBaseSideAmount(), max.getBaseSideAmount());
        MonetaryRange quoteSideLimits = new MonetaryRange(min.getQuoteSideAmount(), max.getQuoteSideAmount());
        Monetary clampedBaseSideAmount = baseSideAmount.clamp(baseSideLimits);
        Monetary clampedQuoteSideAmount = quoteSideAmount.clamp(quoteSideLimits);
        return new TradeAmount(clampedBaseSideAmount, clampedQuoteSideAmount);
    }

    public Monetary getBitcoinSideAmount() {
        if (isBaseSideAmountBitcoin()) {
            return getBaseSideAmount();
        } else {
            return getQuoteSideAmount();
        }
    }
    public Monetary getNonBitcoinSideAmount() {
        if (isQuoteSideAmountBitcoin()) {
            return getBaseSideAmount();
        } else {
            return getQuoteSideAmount();
        }
    }

    public boolean isBaseSideAmountBitcoin() {
        return getBaseSideAmount().isBitcoin();
    }

    public boolean isQuoteSideAmountBitcoin() {
        return getQuoteSideAmount().isBitcoin();
    }

    public int compareToRange(TradeAmountRange limits) {
        return compareToRange(limits.getMin(), limits.getMax());
    }

    public int compareToRange(TradeAmount min, TradeAmount max) {
        MonetaryRange baseSideLimits = new MonetaryRange(min.getBaseSideAmount(), max.getBaseSideAmount());
        MonetaryRange quoteSideLimits = new MonetaryRange(min.getQuoteSideAmount(), max.getQuoteSideAmount());
        int clampedBaseSideAmount = baseSideAmount.compareToRange(baseSideLimits);
        if (clampedBaseSideAmount != 0) {
            return clampedBaseSideAmount;
        }
        return quoteSideAmount.compareToRange(quoteSideLimits);
    }

    public String printRelevantString(Market market, boolean includeCode) {
        if (market.getRelevantCurrencyCode().equals(quoteSideAmount.getCode())) {
            return quoteSideAmount.printAsDouble(includeCode);
        } else {
            return baseSideAmount.printAsDouble(includeCode);
        }
    }

    public String printRelevantString(Market market) {
        return printRelevantString(market, true);
    }
}
