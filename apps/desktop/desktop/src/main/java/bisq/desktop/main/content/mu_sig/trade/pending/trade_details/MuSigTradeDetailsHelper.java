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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_details;

import bisq.bonded_roles.explorer.ExplorerService;
import bisq.common.monetary.Monetary;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsRecords.SecurityDepositInfo;
import bisq.desktop.main.content.mu_sig.trade.pending.trade_details.MuSigTradeDetailsRecords.TradeFeeInfo;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
class MuSigTradeDetailsHelper {

    public static String getBlockExplorerUrl(ExplorerService explorerService, String txId) {
        return explorerService.getExplorerServiceProvider()
                .map(provider ->
                        provider.getBaseUrl() + "/" + provider.getTxPath() + "/" + txId)
                .orElse(Res.get("data.na"));
    }

    // TODO: clone of logic in MuSigMediationCaseDetailSection, consider refactoring to avoid duplication
    public static Optional<SecurityDepositInfo> createSecurityDepositInfo(MuSigContract contract, MuSigTrade trade) {
        MuSigOffer offer = contract.getOffer();
        String tradeId = trade.getId();
        Optional<CollateralOption> collateralOption = OfferOptionUtil.findCollateralOption(offer.getOfferOptions());
        if (collateralOption.isEmpty()) {
            log.warn("CollateralOption not found in offer options. tradeId={}", tradeId);
            return Optional.empty();
        } else if (Math.abs(collateralOption.get().getBuyerSecurityDeposit() - collateralOption.get().getSellerSecurityDeposit()) > 1e-9) {
            log.warn("Buyer and seller security deposits do not match. tradeId={}", tradeId);
            String mismatch = Res.get("authorizedRole.mediator.mediationCaseDetails.securityDepositMismatch");
            return Optional.of(new SecurityDepositInfo(
                    0,
                    mismatch,
                    mismatch,
                    false));
        } else {
            double securityDeposit = collateralOption.get().getBuyerSecurityDeposit();
            return Optional.of(new SecurityDepositInfo(
                    securityDeposit,
                    calculateSecurityDeposit(contract, securityDeposit),
                    PercentageFormatter.formatToPercentWithSymbol(securityDeposit, 0),
                    true));
        }
    }

    public static Optional<TradeFeeInfo> createTradeFeeInfo(MuSigContract contract, MuSigTrade trade) {
        MuSigOffer offer = contract.getOffer();
        Direction takersDirection = offer.getTakersDisplayDirection();
        if (takersDirection.isSell()) {
            return Optional.of(new TradeFeeInfo(Res.get("muSig.offer.taker.review.sellerPaysMinerFee"), "", false));
        } else {
            // TODO: calculate actual trade fee based on offer and trade details, for now we just show a placeholder value
            return Optional.of(new TradeFeeInfo(Res.get("muSig.offer.taker.review.noTradeFees"), "(" + Res.get("data.na") + ")", true));
        }
    }

    public static String calculateSecurityDeposit(MuSigContract contract, double securityDepositAsPercent) {
        Monetary securityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(
                MuSigTradeUtils.getBtcSideMonetary(contract), securityDepositAsPercent);
        return OfferAmountFormatter.formatDepositAmountAsBTC(securityDeposit);
    }
}
