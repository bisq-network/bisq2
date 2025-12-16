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

package bisq.account.age_witness;

import bisq.account.accounts.AccountPayload;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.data.Result;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.ByteArrayUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.security.SignatureUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AccountAgeWitnessService implements Service, DataService.Listener {
    private final NetworkService networkService;

    @Getter
    private final ObservableHashMap<ByteArray, AccountAgeWitness> accountAgeWitnessByHash = new ObservableHashMap<>();

    public AccountAgeWitnessService(NetworkService networkService) {
        this.networkService = networkService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addDataServiceListener(this);
        String storageKey = AuthorizedAccountAgeWitness.class.getSimpleName();
        networkService.getDataService()
                .stream() // turns Optional<DataService> into Stream<DataService>
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedAccountAgeWitness.class::isInstance)
                                .map(AuthorizedAccountAgeWitness.class::cast)
                ).map(e -> e)
                .forEach(this::handleAuthorizedAccountAgeWitnessAdded);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
            handleAuthorizedAccountAgeWitnessAdded(authorizedAccountAgeWitness);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
            handleAuthorizedAccountAgeWitnessRemoved(authorizedAccountAgeWitness);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void registerAccountAgeWitness(AccountAgeWitness accountAgeWitness,
                                        AccountPayload<?> accountPayload,
                                        long peersCurrentTimeMillis,
                                        PublicKey publicKey,
                                        byte[] nonce,
                                        byte[] signature){


        // request from oracles to publish AuthorizedAccountAgeWitness
    }


    public Result<Void> verifyAccountAgeWitness(AccountAgeWitness accountAgeWitness,
                                                AccountPayload<?> accountPayload,
                                                long peersCurrentTimeMillis,
                                                PublicKey publicKey,
                                                byte[] nonce,
                                                byte[] signature) {
        try {
            // Check if peers clock is in a tolerance range of 1 day
            checkArgument(Math.abs(peersCurrentTimeMillis - System.currentTimeMillis()) <= TimeUnit.DAYS.toMillis(1),
                    "Peers clock is more then 1 day off from our clock");

            // Verify that witness hash matches hash created from accountPayload data and publicKey
            byte[] salt = accountPayload.getSalt();
            byte[] ageWitnessInputData = accountPayload.getAgeWitnessInputData();
            byte[] signaturePubKeyBytes = publicKey.getEncoded();
            byte[] hashFromAccountPayload = ByteArrayUtils.concat(ByteArrayUtils.concat(ageWitnessInputData, salt), signaturePubKeyBytes);
            checkArgument(Arrays.equals(accountAgeWitness.getHash(), hashFromAccountPayload),
                    "AgeWitnessHash not matching hashFromAccountPayload");

            SignatureUtil.verify(nonce, signature, publicKey);

            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    public Optional<AccountAgeWitness> findAccountAgeWitness(ByteArray hash) {
        return Optional.ofNullable(accountAgeWitnessByHash.get(hash));
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void handleAuthorizedAccountAgeWitnessAdded(AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
        accountAgeWitnessByHash.putIfAbsent(getAccountAgeWitnessHash(authorizedAccountAgeWitness),
                authorizedAccountAgeWitness.getAccountAgeWitness());
    }

    private void handleAuthorizedAccountAgeWitnessRemoved(AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
        accountAgeWitnessByHash.remove(getAccountAgeWitnessHash(authorizedAccountAgeWitness));
    }

    private static ByteArray getAccountAgeWitnessHash(AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
        return new ByteArray(authorizedAccountAgeWitness.getAccountAgeWitness().getHash());
    }
}
