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

package bisq.trade.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.contract.Party;
import bisq.contract.mu_sig.MuSigContract;

import java.util.Arrays;
import java.util.Optional;

import static bisq.offer.options.OfferOptionUtil.createSaltedAccountPayloadHash;

public final class MuSigTradeUtils {
    public static Monetary getBaseSideMonetary(MuSigTrade trade) {
        return getBaseSideMonetary(trade.getContract());
    }

    public static Monetary getBaseSideMonetary(MuSigContract contract) {
        return Monetary.from(contract.getBaseSideAmount(), contract.getOffer().getMarket().getBaseCurrencyCode());
    }

    public static Monetary getQuoteSideMonetary(MuSigTrade trade) {
        return getQuoteSideMonetary(trade.getContract());
    }

    public static Monetary getQuoteSideMonetary(MuSigContract contract) {
        return Monetary.from(contract.getQuoteSideAmount(), contract.getOffer().getMarket().getQuoteCurrencyCode());
    }

    public static Monetary getBtcSideMonetary(MuSigTrade trade) {
        return getBtcSideMonetary(trade.getContract());
    }

    public static Monetary getBtcSideMonetary(MuSigContract contract) {
        return contract.getOffer().getMarket().isBaseCurrencyBitcoin() ? getBaseSideMonetary(contract) : getQuoteSideMonetary(contract);
    }

    public static Monetary getNonBtcSideMonetary(MuSigTrade trade) {
        return getNonBtcSideMonetary(trade.getContract());
    }

    public static Monetary getNonBtcSideMonetary(MuSigContract contract) {
        return contract.getOffer().getMarket().isBaseCurrencyBitcoin() ? getQuoteSideMonetary(contract) : getBaseSideMonetary(contract);
    }

    public static PriceQuote getPriceQuote(MuSigTrade trade) {
        return PriceQuote.from(getBaseSideMonetary(trade), getQuoteSideMonetary(trade));
    }

    public static PriceQuote getPriceQuote(MuSigContract contract) {
        return PriceQuote.from(getBaseSideMonetary(contract), getQuoteSideMonetary(contract));
    }

    public static boolean doesPeerAccountPayloadMatchContract(MuSigTrade trade, AccountPayload<?> accountPayload) {
        return findPeersContractSaltedAccountPayloadHash(trade)
                .map(expectedHash -> Arrays.equals(expectedHash,
                        createSaltedAccountPayloadHash(accountPayload, trade.getOffer().getId())))
                .orElse(false);
    }

    public static Optional<byte[]> findPeersContractSaltedAccountPayloadHash(MuSigTrade trade) {
        return getPeersContractParty(trade).getSaltedAccountPayloadHash();
    }

    private static Party getPeersContractParty(MuSigTrade trade) {
        MuSigContract contract = trade.getContract();
        return trade.isTaker() ? contract.getMaker() : contract.getTaker();
    }
}
