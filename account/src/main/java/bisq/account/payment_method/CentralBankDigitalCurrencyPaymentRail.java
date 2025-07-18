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

package bisq.account.payment_method;

import lombok.Getter;

//CBDCPaymentRail
public enum CentralBankDigitalCurrencyPaymentRail implements NationalCurrencyPaymentRail {
    // Fully Launched Retail CBDCs
    SAND_DOLLAR("BSD", "BS"),       // Bahamas
    E_NAIRA("NGN", "NG"),          // Nigeria
    JAM_DEX("JMD", "JM"),          // Jamaica

    // Pilot-to-Live Stages
    E_CNY("CNY", "CN"),            // China
    DIGITAL_RUBLE("RUB", "RU"),   // Russia
    E_RUPEE("INR", "IN"),          // India

    // Not yet launched
    US_DIGITAL_DOLLAR("USD", "US"), // Placeholder for future U.S. CBDC (FedNow is not a CBDC)
    DIGITAL_EURO_CBDC("EUR", "EU"); // Eurozone

    @Getter
    private final String peggedCurrency;

    @Getter
    private final String countryCode;

    CentralBankDigitalCurrencyPaymentRail(String peggedCurrency, String countryCode) {
        this.peggedCurrency = peggedCurrency;
        this.countryCode = countryCode;
    }
}
