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

package bisq.chat.channels;

import bisq.user.profile.UserProfile;

public interface PrivateChannel {
    String CHANNEL_DELIMITER = "-";

    UserProfile getPeer();

    static String createChannelId(String peersId, String myId) {
        if (peersId.compareTo(myId) < 0) {
            return peersId + CHANNEL_DELIMITER + myId;
        } else { // need to have an ordering here, otherwise there would be 2 channelIDs for the same participants
            return myId + CHANNEL_DELIMITER + peersId;
        }
    }
}
