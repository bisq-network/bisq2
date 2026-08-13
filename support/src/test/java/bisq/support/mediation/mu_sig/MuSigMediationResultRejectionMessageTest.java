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

package bisq.support.mediation.mu_sig;

import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;

import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.TRADE_ID;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createUserProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigMediationResultRejectionMessageTest {

    @Test
    void roundTripsThroughNetworkMessageResolver() {
        byte[] mediationResultHash = createMediationResultHash();
        MuSigMediationResultRejectionMessage message = createMessage(mediationResultHash);

        Any any = Any.pack(message.resolveValueProto(false));
        ExternalNetworkMessage restored =
                MuSigMediationResultRejectionMessage.getNetworkMessageResolver().fromAny(any);

        assertThat(restored).isEqualTo(message);
        assertThat(((MuSigMediationResultRejectionMessage) restored).getMediationResultHash())
                .containsExactly(mediationResultHash);
    }

    @Test
    void defensivelyCopiesMediationResultHash() {
        byte[] mediationResultHash = createMediationResultHash();
        byte[] expectedHash = mediationResultHash.clone();
        MuSigMediationResultRejectionMessage message = createMessage(mediationResultHash);

        mediationResultHash[0] = 99;
        byte[] returnedHash = message.getMediationResultHash();
        returnedHash[1] = 99;

        assertThat(message.getMediationResultHash()).containsExactly(expectedHash);
    }

    @Test
    void rejectsInvalidMediationResultHash() {
        assertThatThrownBy(() -> createMessage(new byte[19]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createMessage(new byte[21]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MuSigMediationResultRejectionMessage createMessage(byte[] mediationResultHash) {
        UserProfile sender = createUserProfile("sender");
        return new MuSigMediationResultRejectionMessage(
                TRADE_ID,
                sender.getNetworkId(),
                mediationResultHash);
    }

    private static byte[] createMediationResultHash() {
        return new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20
        };
    }
}
