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
import java.util.Optional;

public class CbdcRepository {
    // Fully Launched CBDCs
    public static final Cbdc SAND_DOLLAR = new Cbdc(
            "SD",
            "Sand Dollar",
            "BSD",
            "BS");
    public static final Cbdc E_NAIRA = new Cbdc(
            "eNGN",
            "eNaira",
            "NGN",
            "NG"
    );
    public static final Cbdc JAM_DEX = new Cbdc(
            "JAM-DEX",
            "Jamaica Digital Exchange",
            "JMD",
            "JM"
    );

    // Pilot-Phase CBDCs
    public static final Cbdc E_CNY = new Cbdc(
            "eCNY",
            "Digital Yuan (e-CNY)",
            "CNY",
            "CN"
    );
    public static final Cbdc DIGITAL_RUBLE = new Cbdc(
            "eRUB",
            "Digital Ruble",
            "RUB",
            "RU"
    );
    public static final Cbdc E_RUPEE = new Cbdc(
            "eINR",
            "Digital Rupee (e₹)",
            "INR",
            "IN"
    );

    // Not Yet Launched / Placeholder
    public static final Cbdc US_DIGITAL_DOLLAR = new Cbdc(
            "eUSD",
            "U.S. Digital Dollar",
            "USD",
            "US"
    );
    public static final Cbdc DIGITAL_EURO_CBDC = new Cbdc(
            "eEUR",
            "Digital Euro",
            "EUR",
            "EU"
    );

    public static List<Cbdc> getMajorCbdcs() {
        return List.of(SAND_DOLLAR, E_NAIRA, JAM_DEX, E_CNY, DIGITAL_RUBLE, E_RUPEE);
    }

    public static Optional<Cbdc> find(String code) {
        return getMajorCbdcs().stream()
                .filter(e -> e.getCode().equals(code))
                .findAny();
    }

}
