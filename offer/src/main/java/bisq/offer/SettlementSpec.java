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

import bisq.network.p2p.message.Proto;

/**
 * @param settlementMethodName     Name of SettlementMethod enum 
 * @param makerSettlementAccountId Local ID of maker's settlement account.
 *                                 In case maker had multiple accounts for same settlement method they
 *                                 can define which account to use for that offer
 */
public record SettlementSpec(String settlementMethodName,
                             String makerSettlementAccountId) implements Proto {
}