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

package bisq.oracle_node.bisq1_bridge.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal data for the reputation use case.
 * Need to be in sync with the Bisq 1 BondedReputationDto class.
 */
@Slf4j
@Data
public class BondedReputationDto {
    private long amount;
    private long blockTime;
    private String hash;
    private int blockHeight;
    private int lockTime;
    private String txId;
}