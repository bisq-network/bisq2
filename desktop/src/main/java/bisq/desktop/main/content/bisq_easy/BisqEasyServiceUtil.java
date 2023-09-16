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

package bisq.desktop.main.content.bisq_easy;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;

import java.util.Optional;

public class BisqEasyServiceUtil {
    public static boolean isMaker(ServiceProvider serviceProvider, BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(serviceProvider.getUserService().getUserIdentityService().getMyUserProfileIds());
    }

    public static Optional<BisqEasyTrade> findTradeFromChannel(ServiceProvider serviceProvider, BisqEasyOpenTradeChannel channel) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        boolean maker = isMaker(serviceProvider, bisqEasyOffer);
        UserProfile peerUserProfile = channel.getPeer();
        NetworkId takerNetworkId = maker ?
                peerUserProfile.getNetworkId() :
                myUserIdentity.getUserProfile().getNetworkId();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        return serviceProvider.getTradeService().getBisqEasyTradeService().findTrade(tradeId);
    }
}