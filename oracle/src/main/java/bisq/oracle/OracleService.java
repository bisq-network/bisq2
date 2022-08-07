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

package bisq.oracle;

import bisq.common.application.Service;
import bisq.network.NetworkService;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class OracleService implements Service {
    @Getter
    public static class Config {
        private final String privateKey;
        private final String publicKey;
        private final com.typesafe.config.Config marketPrice;

        public Config(String privateKey,
                      String publicKey,
                      com.typesafe.config.Config marketPrice) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.marketPrice = marketPrice;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getConfig("marketPrice"));
        }
    }

    private final MarketPriceService marketPriceService;

    public OracleService(Config config, String applicationVersion, NetworkService networkService) {
        marketPriceService = new MarketPriceService(MarketPriceService.Config.from(config.getMarketPrice()),
                networkService,
                applicationVersion);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return marketPriceService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }
}