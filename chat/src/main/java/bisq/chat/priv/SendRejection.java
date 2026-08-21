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

package bisq.chat.priv;

/**
 * Why a private chat send is refused locally, before anything is stored or put on the wire. Carried as
 * an enum rather than a message so that each caller phrases it for its own audience — an HTTP status
 * and a popup are not the same sentence.
 *
 * @see PrivateChatChannelService#findSendRejection
 */
public enum SendRejection {
    MY_PROFILE_BANNED,
    PEER_BANNED
}
