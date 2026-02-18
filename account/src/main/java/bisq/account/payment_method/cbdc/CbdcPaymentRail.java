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

package bisq.account.payment_method.cbdc;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.TradeDuration;
import bisq.common.asset.Cbdc;
import bisq.common.asset.CbdcRepository;
import lombok.Getter;

public enum CbdcPaymentRail implements PaymentRail {
    // Fully Launched Retail CBDCs
    SAND_DOLLAR(CbdcRepository.SAND_DOLLAR),
    E_NAIRA(CbdcRepository.E_NAIRA),
    JAM_DEX(CbdcRepository.JAM_DEX),

    // Pilot-to-Live Stages
    E_CNY(CbdcRepository.E_CNY),
    DIGITAL_RUBLE(CbdcRepository.DIGITAL_RUBLE),
    E_RUPEE(CbdcRepository.E_RUPEE),

    // Not yet launched
    US_DIGITAL_DOLLAR(CbdcRepository.US_DIGITAL_DOLLAR),
    DIGITAL_EURO_CBDC(CbdcRepository.DIGITAL_EURO_CBDC);

    @Getter
    private final Cbdc cbdc;

    CbdcPaymentRail(Cbdc cbdc) {
        this.cbdc = cbdc;
    }

    @Override
    public TradeDuration getTradeDuration() {
        return TradeDuration.HOURS_24;
    }
}
