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

package bisq.bonded_roles.registration;

import bisq.common.validation.BitcoinTransactionValidation;

import static com.google.common.base.Preconditions.checkArgument;

public final class BondedRoleRegistrationProtocol {
    public static final int LEGACY_VERSION = 1;
    public static final int PROPOSAL_KEY_VERSION = 2;
    public static final int CURRENT_VERSION = PROPOSAL_KEY_VERSION;

    private BondedRoleRegistrationProtocol() {
    }

    public static boolean isSupported(int version) {
        return version == LEGACY_VERSION || version == PROPOSAL_KEY_VERSION;
    }

    public static int versionFromProto(boolean hasVersion, int version) {
        return hasVersion ? version : LEGACY_VERSION;
    }

    public static void verifyProof(int version,
                                   String proposalTxId,
                                   String lockupTxId) {
        checkArgument(isSupported(version),
                "Unsupported bonded-role registration protocol version: " + version);
        if (version == LEGACY_VERSION) {
            checkArgument(proposalTxId.isEmpty(), "A legacy registration must not contain proposalTxId");
            checkArgument(lockupTxId.isEmpty(), "A legacy registration must not contain lockupTxId");
        } else {
            checkArgument(BitcoinTransactionValidation.isValid(proposalTxId),
                    "proposalTxId must be a 64-character hexadecimal Bitcoin transaction ID");
            checkArgument(BitcoinTransactionValidation.isValid(lockupTxId),
                    "lockupTxId must be a 64-character hexadecimal Bitcoin transaction ID");
        }
    }
}
