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

package bisq.contract;

import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class SignedTwoPartyContract<T extends Offer<?, ?>> {
    private final TwoPartyContract<T> contract;
    private final ContractSignatureData makerSignatureData;
    private final ContractSignatureData takerSignatureData;

    public SignedTwoPartyContract(TwoPartyContract<T> contract, ContractSignatureData makerSignatureData, ContractSignatureData takerSignatureData) {
        this.contract = contract;
        this.makerSignatureData = makerSignatureData;
        this.takerSignatureData = takerSignatureData;
    }
}