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

import bisq.common.util.DateUtils;
import bisq.security.SignatureUtil;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

public interface SignedRequest {
    // Nodes of versions before 2.1.14 create only signature V1, which leaves out several fields of the request. Until
    // this date, a node accepts requests without signature V2. The node compares the date with its own time, not with
    // the creation date of the request: signature V1 of the remove requests does not cover the creation date, so a
    // relay could set an earlier one (see docs/specifications/network/storage-request-signatures.md).
    Date SIGNATURE_V1_CUTOFF_DATE = DateUtils.getUTCDate(2026, GregorianCalendar.NOVEMBER, 25);

    // now: the time of the node which verifies the request
    static boolean isSignatureV2Required(long now) {
        return now >= SIGNATURE_V1_CUTOFF_DATE.getTime();
    }

    byte[] getPreImageV1();

    byte[] getPreImageV2();

    PublicKey getSignaturePublicKey();

    byte[] getSignatureV1();

    byte[] getSignatureV2();

    // Uses the system time. The stores use their clock (see DataStorageService).
    default boolean isSignatureInvalid() {
        return isSignatureInvalid(System.currentTimeMillis());
    }

    // now: the time of the node which verifies the request
    default boolean isSignatureInvalid(long now) {
        if (isSignatureV2Present()) {
            return isSignatureV2Invalid() || isSignatureV1InvalidForEarlierVersions(now);
        }
        return isSignatureV2Required(now) || isSignatureV1Invalid();
    }

    // Before the cutoff date, nodes of earlier versions take part in the network. They verify only signature V1, so a
    // node must not accept, store or serve a request which they reject. Otherwise the owner could add requests with a
    // valid signature V2 and an invalid signature V1, which every inventory response sends again to nodes of earlier
    // versions (see docs/specifications/network/storage-request-signatures.md, verification rule 1).
    // now: the time of the node which verifies the request
    default boolean isSignatureV1InvalidForEarlierVersions(long now) {
        return !isSignatureV2Required(now) && isSignatureV1Invalid();
    }

    default boolean isSignatureV2Present() {
        return getSignatureV2().length > 0;
    }

    // The request is of a node of an earlier version, or a relay removed signature V2
    default boolean isRequiredSignatureV2Missing(long now) {
        return isSignatureV2Required(now) && !isSignatureV2Present();
    }

    // A relay of an earlier version removes signature V2 of a request, and a store can hold that copy. The complete
    // request, which the owner signed with both versions, has the same sequence number as the copy, so the order by
    // sequence number would reject it. It replaces the copy instead: signature V1 of both covers the same preimage, so
    // both are the same request of the same owner, and signature V2 covers the fields which signature V1 leaves out
    // (see docs/specifications/network/storage-request-ordering.md). The caller verifies the signatures of this request
    // before it stores it.
    default boolean isCompleteVersionOf(@Nullable Object storedRequest) {
        return storedRequest instanceof SignedRequest stored &&
                stored.getClass() == getClass() &&
                isSignatureV2Present() &&
                !stored.isSignatureV2Present() &&
                Arrays.equals(getPreImageV1(), stored.getPreImageV1());
    }

    default boolean isSignatureV1Invalid() {
        try {
            return !SignatureUtil.verify(getPreImageV1(), getSignatureV1(), getSignaturePublicKey());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Verifying signature failed", e);
            return true;
        }
    }

    default boolean isSignatureV2Invalid() {
        try {
            return !SignatureUtil.verifyLowS(getPreImageV2(), getSignatureV2(), getSignaturePublicKey());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Verifying signature failed", e);
            return true;
        }
    }
}
