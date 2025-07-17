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

package bisq.common.asset;

import java.util.List;

public class CentralBankDigitalCurrencyRepository {
    // Fully Launched CBDCs
    public static final CentralBankDigitalCurrency SAND_DOLLAR = new CentralBankDigitalCurrency(
            "SD",
            "Sand Dollar",
            "BSD",
            "BS");
    public static final CentralBankDigitalCurrency E_NAIRA = new CentralBankDigitalCurrency(
            "eNGN",
            "eNaira",
            "NGN",
            "NG"
    );
    public static final CentralBankDigitalCurrency JAM_DEX = new CentralBankDigitalCurrency(
            "JAM-DEX",
            "Jamaica Digital Exchange",
            "JMD",
            "JM"
    );

    // Pilot-Phase CBDCs
    public static final CentralBankDigitalCurrency E_CNY = new CentralBankDigitalCurrency(
            "eCNY",
            "Digital Yuan (e-CNY)",
            "CNY",
            "CN"
    );
    public static final CentralBankDigitalCurrency DIGITAL_RUBLE = new CentralBankDigitalCurrency(
            "eRUB",
            "Digital Ruble",
            "RUB",
            "RU"
    );
    public static final CentralBankDigitalCurrency E_RUPEE = new CentralBankDigitalCurrency(
            "eINR",
            "Digital Rupee (e₹)",
            "INR",
            "IN"
    );

    // Not Yet Launched / Placeholder
    public static final CentralBankDigitalCurrency US_DIGITAL_DOLLAR = new CentralBankDigitalCurrency(
            "eUSD",
            "U.S. Digital Dollar",
            "USD",
            "US"
    );
    public static final CentralBankDigitalCurrency DIGITAL_EURO_CBDC = new CentralBankDigitalCurrency(
            "eEUR",
            "Digital Euro",
            "EUR",
            "EU"
    );

    public static List<CentralBankDigitalCurrency> getCentralBankDigitalCurrencies() {
        return List.of(SAND_DOLLAR, E_NAIRA, JAM_DEX, E_CNY, DIGITAL_RUBLE, E_RUPEE);
    }
}
