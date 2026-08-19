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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.common.encoding.Hex;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountAgeProtocolTest {
    private static final String PROFILE_ID = "12".repeat(20);

    @Test
    void missingRequestVersionIsReadAsLegacy() {
        bisq.user.protobuf.AuthorizeAccountAgeRequest proto =
                bisq.user.protobuf.AuthorizeAccountAgeRequest.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setHashAsHex("34".repeat(20))
                        .setDate(System.currentTimeMillis() - 1000)
                        .setPubKeyBase64("key")
                        .setSignatureBase64("signature")
                        .build();

        AuthorizeAccountAgeRequest request = AuthorizeAccountAgeRequest.fromProto(proto);

        assertThat(request.getProtocolVersion()).isEqualTo(AuthorizeAccountAgeRequest.LEGACY_VERSION);
        assertThat(request.getAccountInputDataWithSalt()).isEmpty();
    }

    @Test
    void currentRequestCarriesTheHashPreimage() {
        AuthorizeAccountAgeRequest request = new AuthorizeAccountAgeRequest(
                PROFILE_ID,
                "34".repeat(20),
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));

        AuthorizeAccountAgeRequest roundTrip = AuthorizeAccountAgeRequest.fromProto(request.toValueProto(false));

        assertThat(roundTrip).isEqualTo(request);
        assertThat(roundTrip.getProtocolVersion()).isEqualTo(AuthorizeAccountAgeRequest.CURRENT_VERSION);
        assertThat(roundTrip.getAccountInputDataWithSalt()).containsExactly(1, 2, 3);
    }

    @Test
    void legacyAuthorizedDataRemainsParseableButIsNotCurrent() {
        long date = 1_700_000_000_000L;
        bisq.user.protobuf.AuthorizedAccountAgeData proto =
                bisq.user.protobuf.AuthorizedAccountAgeData.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setDateBucket(date)
                        .setVersion(AuthorizedAccountAgeData.LEGACY_VERSION)
                        .build();

        AuthorizedAccountAgeData data = AuthorizedAccountAgeData.fromProto(proto);

        assertThat(data.isCurrentVersion()).isFalse();
        assertThat(data.getWitnessNullifier()).isEmpty();
        assertThat(data.serializeForHash()).isEqualTo(Hex.decode(
                "0a28" + "3132".repeat(20) + "1080a0abfef962"));
    }

    @Test
    void currentAuthorizedDataSignsTheUnlinkableWitnessIdentity() {
        AuthorizedAccountAgeData data = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);

        AuthorizedAccountAgeData roundTrip = AuthorizedAccountAgeData.fromProto(data.toProto(false));

        assertThat(roundTrip.isCurrentVersion()).isTrue();
        assertThat(roundTrip.getWitnessNullifier())
                .containsExactly(new byte[WitnessReputationProtocol.NULLIFIER_LENGTH]);
    }

    @Test
    void witnessIdentityChangesTheAuthorizedDataHashPayload() {
        long dateBucket = dateBucket();
        AuthorizedAccountAgeData first = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket,
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        byte[] anotherWitnessNullifier = new byte[WitnessReputationProtocol.NULLIFIER_LENGTH];
        anotherWitnessNullifier[0] = 1;
        AuthorizedAccountAgeData second = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket,
                anotherWitnessNullifier,
                false);

        assertThat(first.serializeForHash()).isNotEqualTo(second.serializeForHash());
    }

    @Test
    void currentAccountAgeDataSignsItsVersion() {
        AuthorizedAccountAgeData current = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        AuthorizedAccountAgeData rewrittenAsFuture = AuthorizedAccountAgeData.fromProto(
                current.toProto(false).toBuilder().setVersion(3).build());

        assertThat(current.serializeForHash()).isNotEqualTo(rewrittenAsFuture.serializeForHash());
        assertThat(current).isNotEqualTo(rewrittenAsFuture);
        assertThat(current.hashCode()).isNotEqualTo(rewrittenAsFuture.hashCode());
    }

    @Test
    void currentSignedWitnessRequestCarriesTheHashPreimage() {
        AuthorizeSignedWitnessRequest request = new AuthorizeSignedWitnessRequest(
                PROFILE_ID,
                "34".repeat(20),
                new byte[]{1, 2, 3},
                Base64.getEncoder().encodeToString(new byte[400]),
                Base64.getEncoder().encodeToString(new byte[40]));

        AuthorizeSignedWitnessRequest roundTrip = AuthorizeSignedWitnessRequest.fromProto(
                request.toValueProto(false));

        assertThat(roundTrip).isEqualTo(request);
        assertThat(roundTrip.getProtocolVersion()).isEqualTo(AuthorizeSignedWitnessRequest.CURRENT_VERSION);
        assertThat(roundTrip.getAccountInputDataWithSalt()).containsExactly(1, 2, 3);
    }

    @Test
    void missingSignedWitnessRequestVersionIsReadAsLegacy() {
        bisq.user.protobuf.AuthorizeSignedWitnessRequest proto =
                bisq.user.protobuf.AuthorizeSignedWitnessRequest.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setHashAsHex("34".repeat(20))
                        .setAccountAgeWitnessDate(System.currentTimeMillis() - 2000)
                        .setWitnessSignDate(System.currentTimeMillis() - 1000)
                        .setPubKeyBase64("key")
                        .setSignatureBase64("signature")
                        .build();

        AuthorizeSignedWitnessRequest request = AuthorizeSignedWitnessRequest.fromProto(proto);

        assertThat(request.getProtocolVersion()).isEqualTo(AuthorizeSignedWitnessRequest.LEGACY_VERSION);
        assertThat(request.getAccountInputDataWithSalt()).isEmpty();
    }

    @Test
    void currentSignedWitnessDataSignsTheUnlinkableWitnessIdentity() {
        AuthorizedSignedWitnessData data = new AuthorizedSignedWitnessData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        byte[] anotherWitnessNullifier = new byte[WitnessReputationProtocol.NULLIFIER_LENGTH];
        anotherWitnessNullifier[0] = 1;
        AuthorizedSignedWitnessData another = new AuthorizedSignedWitnessData(
                PROFILE_ID,
                data.getDateBucket(),
                anotherWitnessNullifier,
                false);

        assertThat(AuthorizedSignedWitnessData.fromProto(data.toProto(false)).isCurrentVersion()).isTrue();
        assertThat(data.serializeForHash()).isNotEqualTo(another.serializeForHash());
    }

    @Test
    void currentSignedWitnessDataSignsItsVersion() {
        AuthorizedSignedWitnessData current = new AuthorizedSignedWitnessData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        AuthorizedSignedWitnessData rewrittenAsFuture = AuthorizedSignedWitnessData.fromProto(
                current.toProto(false).toBuilder().setVersion(3).build());

        assertThat(current.serializeForHash()).isNotEqualTo(rewrittenAsFuture.serializeForHash());
        assertThat(current).isNotEqualTo(rewrittenAsFuture);
        assertThat(current.hashCode()).isNotEqualTo(rewrittenAsFuture.hashCode());
    }

    @Test
    void currentWitnessDataRejectsExactDatesAndTwentyByteBisq1Hashes() {
        long exactDate = dateBucket() + 1;

        assertThatThrownBy(() -> new AuthorizedAccountAgeData(
                        PROFILE_ID,
                        exactDate,
                        new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                        false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorizedAccountAgeData(
                        PROFILE_ID,
                        dateBucket(),
                        new byte[20],
                        false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static long dateBucket() {
        long exactDate = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(100);
        return Math.floorDiv(exactDate, WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS) *
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;
    }

}
