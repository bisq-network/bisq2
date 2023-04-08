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

package bisq.wallets.electrum;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.util.NetworkUtils;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.core.WalletService;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.TransactionInfo;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.electrum.notifications.ElectrumNotifyApi;
import bisq.wallets.electrum.notifications.ElectrumNotifyWebServer;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ElectrumWalletService implements WalletService, ElectrumNotifyApi.Listener {
    @Getter
    public static class Config {
        private final String network;
        private final String electrumXServerHost;
        private final int electrumXServerPort;

        public Config(String network, String electrumXServerHost, int electrumXServerPort) {
            this.network = network;
            this.electrumXServerHost = electrumXServerHost;
            this.electrumXServerPort = electrumXServerPort;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getString("network"),
                    config.getString("electrumXServerHost"),
                    config.getInt("electrumXServerPort"));
        }
    }

    private final Config config;
    private final Path electrumRootDataDir;
    private final ElectrumProcessConfig processConfig;
    private final ElectrumProcess electrumProcess;
    private final ElectrumNotifyWebServer electrumNotifyWebServer = new ElectrumNotifyWebServer(NetworkUtils.findFreeSystemPort());

    @Getter
    private final ObservableSet<String> walletAddresses = new ObservableSet<>();
    @Getter
    private final Observable<Coin> balance = new Observable<>(Coin.asBtc(0));
    @Getter
    private final ObservableSet<Transaction> transactions = new ObservableSet<>();
    @Getter
    private boolean isWalletReady;
    private ElectrumWallet wallet;

    public ElectrumWalletService(Config config, Path bisqDataDir) {
        this.config = config;
        electrumRootDataDir = bisqDataDir.resolve("wallets")
                .resolve("electrum");

        processConfig = ElectrumProcessConfig.builder()
                .dataDir(electrumRootDataDir.resolve("wallet"))
                .electrumXServerHost(config.getElectrumXServerHost())
                .electrumXServerPort(config.getElectrumXServerPort())
                .electrumConfig(ElectrumConfig.Generator.generate())
                .build();
        electrumProcess = new ElectrumProcess(electrumRootDataDir, processConfig);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return initializeWallet(processConfig.getElectrumConfig().toRpcConfig(), Optional.empty());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            wallet.shutdown();
            electrumProcess.shutdown();
            electrumNotifyWebServer.stopServer();
            return true;
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // WalletService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initializeWallet(RpcConfig rpcConfig, Optional<String> walletPassphrase) {
        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            electrumProcess.start();

            wallet = new ElectrumWallet(
                    electrumRootDataDir.resolve("wallet")
                            .resolve(config.getNetwork())
                            .resolve("wallets")
                            .resolve("default_wallet"),
                    electrumProcess.getElectrumDaemon()
            );

            wallet.initialize(Optional.empty());
            log.info("Electrum wallet initialized after {} ms.", System.currentTimeMillis() - ts);

            initializeReceiveAddressMonitor();
            requestBalance();
            requestTransactions();
            isWalletReady = true;
            return true;
        });
    }

    @Override
    public CompletableFuture<String> getUnusedAddress() {
        return CompletableFuture.supplyAsync(() -> {
            String receiveAddress = wallet.getUnusedAddress();
            monitorAddress(receiveAddress);
            return receiveAddress;
        });
    }

    @Override
    public CompletableFuture<ObservableSet<String>> requestWalletAddresses() {
        return CompletableFuture.supplyAsync(() -> {
            walletAddresses.addAll(wallet.getWalletAddresses());
            return walletAddresses;
        });
    }

    @Override
    public CompletableFuture<List<? extends TransactionInfo>> listTransactions() {
        return CompletableFuture.supplyAsync(() -> wallet.listTransactions());
    }

    @Override
    public CompletableFuture<ObservableSet<Transaction>> requestTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            transactions.addAll(wallet.getTransactions());
            return transactions;
        });
    }

    @Override
    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        return CompletableFuture.supplyAsync(() -> wallet.listUnspent());
    }

    @Override
    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            String txId = wallet.sendToAddress(passphrase, address, amount);
            // requestBalance();
            return txId;
        });
    }

    @Override
    public CompletableFuture<Boolean> isWalletEncrypted() {
        //todo implement 
        return CompletableFuture.supplyAsync(() -> true);
    }

    @Override
    public CompletableFuture<Coin> requestBalance() {
        return CompletableFuture.supplyAsync(() -> {
            double balance = wallet.getBalance();
            Coin balanceAsCoin = Coin.asBtc(balance);
            this.balance.set(balanceAsCoin);
            return balanceAsCoin;
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //  ElectrumNotifyApi.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAddressStatusChanged(String address, String status) {
        if (status != null) {
            requestBalance();
            requestTransactions();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void initializeReceiveAddressMonitor() {
        ElectrumNotifyApi.addListener(this);
        electrumNotifyWebServer.startServer();
        try {
            requestWalletAddresses().get().forEach(this::monitorAddress);
        } catch (InterruptedException | ExecutionException e) {
            log.error("requestWalletAddresses failed. ", e);
        }
    }

    private void monitorAddress(String address) {
        wallet.notify(address, electrumNotifyWebServer.getNotifyEndpointUrl());
    }


}
