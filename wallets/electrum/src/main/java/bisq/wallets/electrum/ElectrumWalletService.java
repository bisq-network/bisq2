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

import bisq.common.application.Service;
import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.util.NetworkUtils;
import bisq.wallets.core.model.Transaction;
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

@Slf4j
public class ElectrumWalletService implements Service {

    private static final String CURRENCY_CODE = "BTC";

    private final boolean isWalletEnabled;
    private final Path electrumRootDataDir;
    private final ElectrumProcess electrumProcess;
    private final ElectrumNotifyWebServer electrumNotifyWebServer = new ElectrumNotifyWebServer(NetworkUtils.findFreeSystemPort());

    @Getter
    private final Observable<Coin> observableBalanceAsCoin = new Observable<>(Coin.of(0, CURRENCY_CODE));
    @Getter
    private final ObservableSet<String> receiveAddresses = new ObservableSet<>();

    private ElectrumWallet electrumWallet;

    public ElectrumWalletService(boolean isWalletEnabled, Path bisqDataDir) {
        this.isWalletEnabled = isWalletEnabled;
        electrumRootDataDir = bisqDataDir.resolve("wallets")
                .resolve("electrum");
        this.electrumProcess = createElectrumProcess();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!isWalletEnabled) {
            return CompletableFuture.completedFuture(true);
        }

        log.info("initialize");
        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            electrumProcess.start();

            electrumWallet = new ElectrumWallet(
                    electrumRootDataDir.resolve("wallet")
                            .resolve("regtest")
                            .resolve("wallets")
                            .resolve("default_wallet"),
                    electrumProcess.getElectrumDaemon(),
                    new ObservableSet<>()
            );

            // TODO pw support
            electrumWallet.initialize(Optional.empty());
            log.info("Electrum wallet initialized after {} ms.", System.currentTimeMillis() - ts);

            initializeReceiveAddressMonitor();
            updateBalance();

            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (!isWalletEnabled) {
            return CompletableFuture.completedFuture(true);
        }

        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            electrumWallet.shutdown();
            electrumProcess.shutdown();
            electrumNotifyWebServer.stopServer();
            return true;
        });
    }

    public CompletableFuture<String> getNewAddress() {
        return CompletableFuture.supplyAsync(() -> {
            String receiveAddress = electrumWallet.getNewAddress();
            monitorAddress(receiveAddress);

            // Do we need persistence?
            receiveAddresses.add(receiveAddress);
            return receiveAddress;
        });
    }

    public CompletableFuture<List<? extends Transaction>> listTransactions() {
        return CompletableFuture.supplyAsync(() -> electrumWallet.listTransactions());
    }

    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        return CompletableFuture.supplyAsync(() -> electrumWallet.listUnspent());
    }

    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            String txId = electrumWallet.sendToAddress(passphrase, address, amount);
            updateBalance();
            return txId;
        });
    }

    private ElectrumProcess createElectrumProcess() {
        var processConfig = ElectrumProcessConfig.builder()
                .dataDir(electrumRootDataDir.resolve("wallet"))
                .electrumXServerHost("127.0.0.1")
                .electrumXServerPort(50001)
                .electrumConfig(ElectrumConfig.Generator.generate())
                .build();

        return new ElectrumProcess(electrumRootDataDir, processConfig);
    }

    private void initializeReceiveAddressMonitor() {
        ElectrumNotifyApi.registerListener((address, status) -> {
            if (status != null) {
                receiveAddresses.remove(address);
                updateBalance();
            }
        });

        electrumNotifyWebServer.startServer();
        receiveAddresses.forEach(address ->
                electrumWallet.notify(address, electrumNotifyWebServer.getNotifyEndpointUrl())
        );
    }

    private void monitorAddress(String address) {
        electrumWallet.notify(address, electrumNotifyWebServer.getNotifyEndpointUrl());
    }

    private void updateBalance() {
        CompletableFuture.runAsync(() -> {
            double balance = electrumWallet.getBalance();
            Coin coin = Coin.of(balance, CURRENCY_CODE);

            // Balance changed?
            if (!observableBalanceAsCoin.get().equals(coin)) {
                observableBalanceAsCoin.set(coin);
            }
        });
    }
}
