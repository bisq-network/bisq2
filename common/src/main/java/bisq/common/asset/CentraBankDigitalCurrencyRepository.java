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

public class CentraBankDigitalCurrencyRepository {
    // Fully Launched CBDCs
    public static final CentraBankDigitalCurrency SAND_DOLLAR = new CentraBankDigitalCurrency(
            "SD",
            "Sand Dollar",
            "BSD",
            "BS");
    public static final CentraBankDigitalCurrency E_NAIRA = new CentraBankDigitalCurrency(
            "eNGN",
            "eNaira",
            "NGN",
            "NG"
    );
    public static final CentraBankDigitalCurrency JAM_DEX = new CentraBankDigitalCurrency(
            "JAM-DEX",
            "Jamaica Digital Exchange",
            "JMD",
            "JM"
    );

    // Pilot-Phase CBDCs
    public static final CentraBankDigitalCurrency E_CNY = new CentraBankDigitalCurrency(
            "eCNY",
            "Digital Yuan (e-CNY)",
            "CNY",
            "CN"
    );
    public static final CentraBankDigitalCurrency DIGITAL_RUBLE = new CentraBankDigitalCurrency(
            "eRUB",
            "Digital Ruble",
            "RUB",
            "RU"
    );
    public static final CentraBankDigitalCurrency E_RUPEE = new CentraBankDigitalCurrency(
            "eINR",
            "Digital Rupee (e₹)",
            "INR",
            "IN"
    );

    // Not Yet Launched / Placeholder
    public static final CentraBankDigitalCurrency US_DIGITAL_DOLLAR = new CentraBankDigitalCurrency(
            "eUSD",
            "U.S. Digital Dollar",
            "USD",
            "US"
    );
    public static final CentraBankDigitalCurrency DIGITAL_EURO_CBDC = new CentraBankDigitalCurrency(
            "eEUR",
            "Digital Euro",
            "EUR",
            "EU"
    );

    public static List<CentraBankDigitalCurrency> getCentraBankDigitalCurrencies() {
        return List.of(SAND_DOLLAR, E_NAIRA, JAM_DEX, E_CNY, DIGITAL_RUBLE, E_RUPEE);
    }
}
