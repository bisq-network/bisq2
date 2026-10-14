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

package bisq.desktop.main.content.mu_sig;

import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
public class MusigTradesUtils {

    public static int getPreviousMusigTradesWithPeer(
                                                 String peerUserProfile,
                                                 MuSigTradeService musigTradeService) {
        Collection<MuSigTrade> muSigTrades = musigTradeService.getTrades();
        return (int) muSigTrades.stream().filter(trade -> {
            String tradePeer = trade.getPeer().getNetworkId().getId();
            return trade.getTradeCompletedDate().isPresent()
                    && trade.getTradeCompletedDate().get() < System.currentTimeMillis()
            && tradePeer.equals(peerUserProfile);
        }).count();
    }
}
