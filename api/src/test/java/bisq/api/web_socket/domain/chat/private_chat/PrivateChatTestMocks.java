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

package bisq.api.web_socket.domain.chat.private_chat;

import bisq.user.profile.UserProfile;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mocks shared by the private-chat WebSocket service tests, all of which push mapped dtos. */
final class PrivateChatTestMocks {
    private PrivateChatTestMocks() {
    }

    /**
     * A deep stub is not enough on its own: {@code DtoMappings.UserProfileMapping} runs the profile through
     * Base64 and a digest, and a stubbed {@code byte[]} getter hands back null. The services catch and log
     * the resulting NPE, so without these stubs a mapping failure and a service that decided not to push
     * look exactly the same from the outside.
     */
    static UserProfile mockUserProfile() {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getNickName()).thenReturn("peer");
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }
}
