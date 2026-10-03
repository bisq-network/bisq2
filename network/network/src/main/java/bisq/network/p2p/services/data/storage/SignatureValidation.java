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

package bisq.network.p2p.services.data.storage;

import bisq.common.validation.NetworkDataValidation;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;

public class SignatureValidation {

    public static void validateSignatures(byte[] signatureV1, byte[] signatureV2) {
        validateSignatures(signatureV1, signatureV2, System.currentTimeMillis());
    }

    // The constructors of the requests call this method, so it also runs for a request which is not received:
    // - for the request without signature V2 which the creator builds to compute the preimage of signature V2;
    // - for the persisted requests which a store loads. The mailbox store keeps removals without signature V2 after
    //   the cutoff date of signature V1.
    // The method therefore does not require signature V2. The stores reject a received request without signature V2
    // from the cutoff date on, with their clock, and they remove such requests which they hold (see rules 5 and 9 of
    // docs/specifications/network/storage-request-signatures.md).
    // now: the time of the node which parses the request
    @VisibleForTesting
    static void validateSignatures(byte[] signatureV1, byte[] signatureV2, long now) {
        checkArgument(signatureV1.length > 0 || signatureV2.length > 0, "A request must contain a signature");
        // Before the cutoff date nodes of earlier versions take part in the network. They cannot parse a request
        // without signature V1. After the cutoff date we might omit the v1 signature.
        if (signatureV1.length > 0 || !SignedRequest.isSignatureV2Required(now)) {
            NetworkDataValidation.validateECSignature(signatureV1);
        }
        if (signatureV2.length > 0) {
            NetworkDataValidation.validateECSignature(signatureV2);
        }
    }

    // Version 0 is the format of releases before the version field existed. Version 2 is the first version with
    // signature V2, and every node which signs with signature V2 sets its version. A request with signature V2 and
    // version 0 therefore comes from no supported sender.
    public static void validateVersionOfSignatureV2(byte[] signatureV2, int version) {
        checkArgument(signatureV2.length == 0 || version != 0, "A request with signature V2 must not have version 0");
    }
}
