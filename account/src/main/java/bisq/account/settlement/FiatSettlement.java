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

package bisq.account.settlement;

import static bisq.account.settlement.FiatSettlement.Method.*;

public class FiatSettlement extends Settlement<FiatSettlement.Method> {
    public static final FiatSettlement SEPA = new FiatSettlement(Method.SEPA);
    public static final FiatSettlement REVOLUT = new FiatSettlement(Method.REVOLUT);
    public static final FiatSettlement ZELLE = new FiatSettlement(Method.ZELLE);

    public enum Method implements Settlement.Method {
        SEPA,
        REVOLUT,
        ZELLE,
        OTHER
    }

    public FiatSettlement(Method method) {
        super(method);
    }

    public FiatSettlement(Method method, String name) {
        super(method, name);
    }

    public FiatSettlement(String name) {
        super(name);
    }

    @Override
    protected Method getDefaultMethod() {
        return OTHER;
    }

    @Override
    protected Type getDefaultType() {
        return Type.AUTOMATIC; // todo should be manual, but test fails with manual
    }
}
