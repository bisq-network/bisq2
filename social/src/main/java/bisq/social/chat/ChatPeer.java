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

package bisq.social.chat;

import bisq.common.encoding.Hex;
import bisq.network.NetworkId;
import bisq.security.DigestUtil;

import java.io.Serializable;
// The userName is not safe against impersonation attacks, so we have to combine it with some easy to remember 
// addition.

// The ID can be used to derive a unique identifier (e.g. emojiId - https://rfc.tari.com/RFC-0152_EmojiId.html
// Emoji java lib: https://github.com/Coding/emoji-java)
// Alternatively we could use a short sentence/poem deterministically created from the ID 
// E.g. [userName] - the [color] [adjective/adverb] [verb] [noun]: 
// Example: "John - the blue slowly wandering glacier". Will result mostly in nonsense but might be good enough 
// to remember and detect an impersonator.
public record ChatPeer(String userName, NetworkId networkId) implements Serializable {
    public String id() {
        return Hex.encode(DigestUtil.hash(networkId.getPubKey().publicKey().getEncoded()));
    }
}