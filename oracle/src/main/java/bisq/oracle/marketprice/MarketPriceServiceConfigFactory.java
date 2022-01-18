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

package bisq.oracle.marketprice;

import bisq.network.p2p.node.transport.Transport;

import java.util.Set;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
public class MarketPriceServiceConfigFactory {
    public static MarketPriceService.Config getConfig() {
        return new MarketPriceService.Config(Set.of(
                new MarketPriceService.Provider("https://price.bisq.wiz.biz/", "wiz", Transport.Type.CLEAR),
                new MarketPriceService.Provider("http://wizpriceje6q5tdrxkyiazsgu7irquiqjy2dptezqhrtu7l2qelqktid.onion/", "wiz", Transport.Type.TOR),
                new MarketPriceService.Provider("http://emzypricpidesmyqg2hc6dkwitqzaxrqnpkdg3ae2wef5znncu2ambqd.onion/", "emzy", Transport.Type.TOR),
                new MarketPriceService.Provider("http://aprcndeiwdrkbf4fq7iozxbd27dl72oeo76n7zmjwdi4z34agdrnheyd.onion/", "mrosseel", Transport.Type.TOR),
                new MarketPriceService.Provider("http://devinpndvdwll4wiqcyq5e7itezmarg7rzicrvf6brzkwxdm374kmmyd.onion/", "devinbileck", Transport.Type.TOR),
                new MarketPriceService.Provider("http://ro7nv73awqs3ga2qtqeqawrjpbxwarsazznszvr6whv7tes5ehffopid.onion/", "alexej996", Transport.Type.TOR)));
    }
}