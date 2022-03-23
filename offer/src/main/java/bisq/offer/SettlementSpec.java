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

package bisq.offer;

import bisq.common.encoding.Proto;

import javax.annotation.Nullable;

/**
 * @param settlementMethodName Name of SettlementMethod enum
 * @param makerAccountId       Local ID of maker's settlement account.
 *                             In case maker had multiple accounts for same settlement method they
 *                             can define which account to use for that offer
 */
//todo use salt for makerAccountId to avoid leaking privacy, otherwise observers can derive which offer is the same 
// maker if same account is used in multiple offer. 
    //todo move makerAccountId to openOffer
public record SettlementSpec(String settlementMethodName,
                            @Nullable String makerAccountId) implements Proto {
}