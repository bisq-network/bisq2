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

package bisq.bonded_roles.explorer;

import bisq.bonded_roles.explorer.dto.Tx;
import bisq.common.data.Pair;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.TransportType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class ExplorerService {
    public static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("BlockExplorerService.pool", 3);

    private volatile boolean shutdownStarted;

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config config) {
            //todo move to conf
            return new Config(List.of(
                    //https://mempool.space/api/tx/15e10745f15593a899cef391191bdd3d7c12412cc4696b7bcb669d0feadc8521
                    new Provider("https://mempool.emzy.de/", TransportType.CLEAR),
                    new Provider("http://mempool4t6mypeemozyterviq3i5de4kpoua65r3qkn5i3kknu5l2cad.onion/", TransportType.TOR)

                   /* new BlockchainExplorerService.Provider("https://mempool.space/tx/", "https://mempool.space/address/", "mempool.space (@wiz)", Transport.Type.CLEAR),
                    new BlockchainExplorerService.Provider("http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/tx/", "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/address/", "mempool.space Tor V3", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://mempool.bisq.services/tx/", "https://mempool.bisq.services/address/", "mempool.bisq.services (@devinbileck)", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("http://mempoolcutehjtynu4k4rd746acmssvj2vz4jbz4setb72clbpx2dfqd.onion/tx/", "http://mempoolcutehjtynu4k4rd746acmssvj2vz4jbz4setb72clbpx2dfqd.onion/address/", "mempool.bisq.services Tor V3", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://blockstream.info/tx/", "https://blockstream.info/address/", "Blockstream.info", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion/tx/", "http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion/address/", "Blockstream.info Tor V3", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://oxt.me/transaction/", "https://oxt.me/address/", "OXT", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://bitaps.com/", "https://bitaps.com/", "Bitaps", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://live.blockcypher.com/btc/tx/", "https://live.blockcypher.com/btc/address/", "Blockcypher", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://tradeblock.com/bitcoin/tx/", "https://tradeblock.com/bitcoin/address/", "Tradeblock", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://www.biteasy.com/transactions/", "https://www.biteasy.com/addresses/", "Biteasy", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://www.blockonomics.co/api/tx?txid=", "https://www.blockonomics.co/#/search?q=", "Blockonomics", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("http://chainflyer.bitflyer.jp/Transaction/", "http://chainflyer.bitflyer.jp/Address/", "Chainflyer", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://www.smartbit.com.au/tx/", "https://www.smartbit.com.au/address/", "Smartbit", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://chain.so/tx/BTC/", "https://chain.so/address/BTC/", "SoChain. Wow.", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://blockchain.info/tx/", "https://blockchain.info/address/", "Blockchain.info", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://insight.bitpay.com/tx/", "https://insight.bitpay.com/address/", "Insight", Transport.Type.TOR),
                    new BlockchainExplorerService.Provider("https://blockchair.com/bitcoin/transaction/", "https://blockchair.com/bitcoin/address/", "Blockchair", Transport.Type.TOR)*/
            ));
        }

        private final List<Provider> providers;

        public Config(List<Provider> providers) {
            this.providers = providers;
        }
    }

    private static class PendingRequestException extends Exception {
        public PendingRequestException() {
            super("We have a pending request");
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String baseUrl;
        private final String apiPath;
        private final String txPath;
        private final String addressPath;
        private final TransportType transportType;

        public Provider(String baseUrl, TransportType transportType) {
            this(baseUrl, "api/", "tx/", "address/", transportType);
        }

        public Provider(String baseUrl, String apiPath, String txPath, String addressPath, TransportType transportType) {
            this.baseUrl = baseUrl;
            this.apiPath = apiPath;
            this.txPath = txPath;
            this.addressPath = addressPath;
            this.transportType = transportType;
        }
    }


    private final ArrayList<Provider> providers;
    @Getter
    private final Observable<Provider> selectedProvider = new Observable<>();
    private Optional<BaseHttpClient> httpClient = Optional.empty();
    private final NetworkService networkService;
    private final String userAgent;


    public ExplorerService(Config conf, NetworkService networkService, Version version) {
        providers = new ArrayList<>(conf.providers);
        checkArgument(providers.size() > 0);
        selectedProvider.set(providers.get(0));
        this.networkService = networkService;
        userAgent = "bisq-v2/" + version.toString();
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        shutdownStarted = true;
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        Provider provider = selectedProvider.get();
        BaseHttpClient httpClient = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);

        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            String param = provider.getApiPath() + provider.getTxPath() + txId;
            try {
                String json = httpClient.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                log.info("Requesting tx from {} took {} ms", httpClient.getBaseUrl() + param, System.currentTimeMillis() - ts);
                return new ObjectMapper().readValue(json, Tx.class);
            } catch (IOException e) {
                if (!shutdownStarted) {
                    log.info("Requesting tx from {} failed. {}" + httpClient.getBaseUrl() + param, ExceptionUtil.print(e));
                }
                throw new RuntimeException(e);
            }
        }, POOL);
    }
}
