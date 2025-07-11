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

package bisq.wallets.bitcoind;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class BitcoinWalletService extends AbstractBitcoindWalletService<BitcoinWallet, BitcoinWalletStore> {
    @Getter
    public static class Config {
        private final String network;

        public Config(String network) {
            this.network = network;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getString("network"));
        }

        public boolean isRegtest() {
            return network.equals("regtest");
        }
    }

    private final Config config;
    private final BitcoinWalletStore persistableStore = new BitcoinWalletStore();
    private final Persistence<BitcoinWalletStore> persistence;
    private final Observable<Coin> balance = new Observable<>(Coin.asBtcFromValue(0));
    private final Observable<Boolean> isWalletInitialized = new Observable<>(true);
    private final Observable<Boolean> isWalletBackedUp = new Observable<>(true);

    public BitcoinWalletService(Config config,
                                PersistenceService persistenceService) {
        super("BTC", getOptionalRegtestConfig(config.isRegtest(), 18443), "bisq_bitcoind_default_wallet");
        this.config = config;
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.WALLETS, persistableStore);
    }

    @Override
    protected BitcoinWallet createWallet(RpcConfig rpcConfig) {
        return WalletFactory.createBitcoinWallet(rpcConfig.toJsonRpcConfig(), walletName, persistableStore);
    }

    @Override
    protected void persistRpcConfig(RpcConfig rpcConfig) {
        persistableStore.setRpcConfig(Optional.of(rpcConfig));
        persist();
    }

    @Override
    protected Optional<RpcConfig> getRpcConfigFromPersistableStore() {
        return persistableStore.getRpcConfig();
    }

    @Override
    public Observable<Boolean> getIsWalletInitialized() {
        return isWalletInitialized;
    }

    @Override
    public Observable<Boolean> getIsWalletBackedup() {
        return isWalletBackedUp;
    }

    @Override
    public void setIsWalletBackedup(Boolean value) {
        isWalletBackedUp.set(value);
    }

    @Override
    public CompletableFuture<Coin> requestBalance() {
        return wallet.map(bitcoinWallet -> CompletableFuture.supplyAsync(() -> {
            double balance = bitcoinWallet.getBalance();
            Coin balanceAsCoin = Coin.asBtcFromFaceValue(balance);
            this.balance.set(balanceAsCoin);
            return balanceAsCoin;
        })).orElseGet(() -> CompletableFuture.completedFuture(Coin.asBtcFromValue(0)));
    }

    @Override
    public void setNoEncryption() {

    }

    @Override
    public void setEncryptionPassword(String password) {
        log.debug("setEncryptionPassword called ");
    }

    // TODO
    @Override
    public CompletableFuture<List<String>> getSeedWords() {
        return CompletableFuture.supplyAsync(() ->
                        Arrays.asList("car", "van", "lion", "water", "bero", "cycle",
                                "love", "key", "system", "wife", "husband", "trade"),
                CompletableFuture.delayedExecutor(400, TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public void purgeSeedWords() {

    }
}
