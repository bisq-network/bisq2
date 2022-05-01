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

package bisq.oracle.daobridge.dto.dao;

import lombok.Getter;

@Getter
public enum TxType {
    UNDEFINED(false, false), // only fallback for backward compatibility in case we add a new value and old clients fall back to UNDEFINED
    UNDEFINED_TX_TYPE(false, false),
    UNVERIFIED(false, false),
    INVALID(false, false),
    GENESIS(false, false),
    TRANSFER_BSQ(false, false),
    PAY_TRADE_FEE(false, true),
    PROPOSAL(true, true),
    COMPENSATION_REQUEST(true, true),
    REIMBURSEMENT_REQUEST(true, true),
    BLIND_VOTE(true, true),
    VOTE_REVEAL(true, false),
    LOCKUP(true, false),
    UNLOCK(true, false),
    ASSET_LISTING_FEE(true, true),
    PROOF_OF_BURN(true, true),
    IRREGULAR(false, false); // the params are irrelevant here as we can have any tx that violated the rules set to irregular


    @Getter
    private final boolean hasOpReturn;
    @Getter
    private final boolean requiresFee;

    TxType(boolean hasOpReturn, boolean requiresFee) {
        this.hasOpReturn = hasOpReturn;
        this.requiresFee = requiresFee;
    }
}
