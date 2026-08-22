/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.encoding.Hex;
import bisq.user.reputation.WitnessReputationProtocol;

import com.google.protobuf.CodedOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WitnessOwnershipResponseMessagesTest {
    private static final long GOLDEN_DATE_BUCKET = 1_699_920_000_000L;
    private static final byte[] GOLDEN_NULLIFIER = Hex.decode(
            "f8595c82649513a9df373f4d14077922585b6ed555fdabc0c6c4e6967aa4f563");

    @Test
    void accountAgeResponseRoundTripsBucketAndNullifier() {
        AccountAgeWitnessOwnershipResponse response =
                new AccountAgeWitnessOwnershipResponse(GOLDEN_DATE_BUCKET, GOLDEN_NULLIFIER);

        AccountAgeWitnessOwnershipResponse roundTrip =
                AccountAgeWitnessOwnershipResponse.fromProto(response.toProto(false));

        assertThat(roundTrip).isEqualTo(response);
        assertThat(roundTrip.getDateBucket()).isEqualTo(GOLDEN_DATE_BUCKET);
        assertThat(roundTrip.getWitnessNullifier()).containsExactly(GOLDEN_NULLIFIER);
    }

    @Test
    void signedWitnessResponseRoundTripsBucketAndNullifier() {
        SignedWitnessOwnershipResponse response =
                new SignedWitnessOwnershipResponse(GOLDEN_DATE_BUCKET, GOLDEN_NULLIFIER);

        SignedWitnessOwnershipResponse roundTrip =
                SignedWitnessOwnershipResponse.fromProto(response.toProto(false));

        assertThat(roundTrip).isEqualTo(response);
        assertThat(roundTrip.getDateBucket()).isEqualTo(GOLDEN_DATE_BUCKET);
        assertThat(roundTrip.getWitnessNullifier()).containsExactly(GOLDEN_NULLIFIER);
    }

    @Test
    void responseRejectsAnExactDateOrRawBisq1Hash() {
        assertThatThrownBy(() -> new AccountAgeWitnessOwnershipResponse(
                        dateBucket() + 1,
                        new byte[WitnessReputationProtocol.NULLIFIER_LENGTH]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignedWitnessOwnershipResponse(
                        dateBucket(),
                        new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void responseDoesNotExposeItsMutableNullifierArray() {
        byte[] nullifier = new byte[WitnessReputationProtocol.NULLIFIER_LENGTH];
        AccountAgeWitnessOwnershipResponse response =
                new AccountAgeWitnessOwnershipResponse(dateBucket(), nullifier);

        nullifier[0] = 1;
        byte[] returned = response.getWitnessNullifier();
        returned[1] = 2;

        assertThat(response.getWitnessNullifier())
                .containsExactly(new byte[WitnessReputationProtocol.NULLIFIER_LENGTH]);
    }

    @Test
    void legacyExactDateResponsesFailClosed() throws IOException {
        byte[] legacyResponse = legacyResponse(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100));

        var accountAgeProto = bisq.bridge.protobuf.AccountAgeWitnessOwnershipResponse.parseFrom(legacyResponse);
        var signedWitnessProto = bisq.bridge.protobuf.SignedWitnessOwnershipResponse.parseFrom(legacyResponse);

        assertThatThrownBy(() -> AccountAgeWitnessOwnershipResponse.fromProto(accountAgeProto))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SignedWitnessOwnershipResponse.fromProto(signedWitnessProto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static long dateBucket() {
        long date = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100);
        return Math.floorDiv(date, WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS) *
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;
    }

    private static byte[] legacyResponse(long exactDate) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        codedOutputStream.writeSInt64(1, exactDate);
        codedOutputStream.flush();
        return outputStream.toByteArray();
    }
}
