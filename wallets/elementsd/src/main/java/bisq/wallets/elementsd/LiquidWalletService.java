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

package bisq.wallets.elementsd;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.wallets.bitcoind.AbstractBitcoindWalletService;
import bisq.wallets.bitcoind.RpcConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LiquidWalletService extends AbstractBitcoindWalletService<LiquidWallet, LiquidWalletStore> {

    @Getter
    private final LiquidWalletStore persistableStore = new LiquidWalletStore();
    @Getter
    private final Persistence<LiquidWalletStore> persistence;
    @Getter
    private final Observable<Coin> balance = new Observable<>(Coin.fromValue(0, "L-BTC"));

    public LiquidWalletService(PersistenceService persistenceService,
                               boolean isRegtest) {
        super("L-BTC", getOptionalRegtestConfig(isRegtest, 7040), "bisq_elements_default_wallet");
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.WALLETS, persistableStore);
    }

    @Override
    protected LiquidWallet createWallet(bisq.wallets.bitcoind.RpcConfig rpcConfig) {
        return WalletFactory.createLiquidWallet(rpcConfig.toJsonRpcConfig(), walletName, persistableStore);
    }

    @Override
    protected void persistRpcConfig(bisq.wallets.bitcoind.RpcConfig rpcConfig) {
        persistableStore.setRpcConfig(Optional.of(rpcConfig.toJsonRpcConfig()));
        persist();
    }

    @Override
    protected Optional<bisq.wallets.bitcoind.RpcConfig> getRpcConfigFromPersistableStore() {
        return persistableStore.getRpcConfig().map(jsonRpcConfig ->
                RpcConfig.builder()
                        .hostname(jsonRpcConfig.getHostname())
                        .port(jsonRpcConfig.getPort())
                        .user(jsonRpcConfig.getUser())
                        .password(jsonRpcConfig.getPassword())
                        .build());
    }

    @Override
    public CompletableFuture<Coin> requestBalance() {
        //todo impl wallet.getBalance request
        return CompletableFuture.completedFuture(balance.get());
    }
}
