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

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.data.Result;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.ByteArrayUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AccountAgeWitnessService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    @Getter
    private final ObservableHashMap<ByteArray, AccountAgeWitness> accountAgeWitnessByHash = new ObservableHashMap<>();

    public AccountAgeWitnessService(NetworkService networkService,
                                    UserService userService,
                                    BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        this.authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
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

    public void handleAddedAccount(Account<?, ?> account) {
        if (requireRegistration(account)) {
            registerAccountAgeWitness(account);
        }
    }

    public void registerAccountAgeWitness(Account<?, ?> account) {
        long now = System.currentTimeMillis();
        AccountPayload<?> accountPayload = account.getAccountPayload();
        byte[] blindedAgeWitnessInputData = getBlindedAgeWitnessInputData(accountPayload);
        KeyPair keyPair = account.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] bytes = ByteArrayUtils.concat(blindedAgeWitnessInputData, publicKeyBytes);
        byte[] hash = DigestUtil.hash(bytes);
        KeyAlgorithm keyAlgorithm = account.getKeyAlgorithm();

        AccountAgeWitness accountAgeWitness = new AccountAgeWitness(hash, System.currentTimeMillis());
        byte[] message = accountAgeWitness.toProto(true).toByteArray();
        try {
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), keyAlgorithm.getAlgorithm());
            AuthorizeAccountAgeWitnessRequest authorizeAccountAgeWitnessRequest = new AuthorizeAccountAgeWitnessRequest(accountAgeWitness,
                    blindedAgeWitnessInputData,
                    publicKeyBytes,
                    signature,
                    keyAlgorithm,
                    now);

            authorizedBondedRolesService.getAuthorizedOracleNodes()
                    .forEach(oracleNode ->
                            networkService.confidentialSend(authorizeAccountAgeWitnessRequest,
                                    oracleNode.getNetworkId(),
                                    userIdentityService.getSelectedUserIdentity().getNetworkIdWithKeyPair()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public Result<Void> verifyAccountAgeWitness(AccountAgeWitness accountAgeWitness,
                                                AccountPayload<?> accountPayload,
                                                long peersCurrentTimeMillis,
                                                PublicKey publicKey,
                                                byte[] message,
                                                byte[] signature,
                                                KeyAlgorithm keyAlgorithm) {
        try {
            // Check if peers clock is in a tolerance range of 1 day
            checkArgument(Math.abs(peersCurrentTimeMillis - System.currentTimeMillis()) <= TimeUnit.DAYS.toMillis(1),
                    "Peers clock is more then 1 day off from our clock");
            byte[] signaturePubKeyBytes = publicKey.getEncoded();
            byte[] blindedAgeWitnessInputData = getBlindedAgeWitnessInputData(accountPayload);
            byte[] hashFromAccountPayload = ByteArrayUtils.concat(blindedAgeWitnessInputData, signaturePubKeyBytes);
            checkArgument(Arrays.equals(accountAgeWitness.getHash(), hashFromAccountPayload),
                    "AgeWitnessHash not matching hashFromAccountPayload");
            checkArgument(SignatureUtil.verify(message, signature, publicKey, keyAlgorithm.getAlgorithm()), "Signature verification failed");
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(e);
        }
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


    private boolean requireRegistration(Account<? extends PaymentMethod<?>, ?> account) {
        byte[] hash = createHash(account);
        return findAuthorizedAccountAgeWitness(hash)
                .map(AccountAgeWitnessService::isHalfExpired)
                .findAny()
                .orElse(true);
    }

    private static byte[] getBlindedAgeWitnessInputData(AccountPayload<?> accountPayload) {
        byte[] salt = accountPayload.getSalt();
        byte[] ageWitnessInputData = accountPayload.getAgeWitnessInputData();
        return ByteArrayUtils.concat(ageWitnessInputData, salt);
    }

    public Optional<AccountAgeWitness> findAccountAgeWitness(ByteArray hash) {
        return Optional.ofNullable(accountAgeWitnessByHash.get(hash));
    }

    private byte[] createHash(Account<? extends PaymentMethod<?>, ?> account) {
        AccountPayload<?> accountPayload = account.getAccountPayload();
        byte[] salt = accountPayload.getSalt();
        byte[] ageWitnessInputData = accountPayload.getAgeWitnessInputData();
        byte[] blindedAgeWitnessInputData = ByteArrayUtils.concat(ageWitnessInputData, salt);
        KeyPair keyPair = account.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        return ByteArrayUtils.concat(blindedAgeWitnessInputData, publicKeyBytes);
    }

    private static boolean isHalfExpired(AuthorizedAccountAgeWitness authorizedAccountAgeWitness) {
        long ttl = authorizedAccountAgeWitness.getMetaData().getTtl();
        long halfExpiryDate = System.currentTimeMillis() - ttl / 2;
        return authorizedAccountAgeWitness.getPublishDate() <= halfExpiryDate;
    }

    private Stream<AuthorizedAccountAgeWitness> findAuthorizedAccountAgeWitness(byte[] hash) {
        String storageKey = AuthorizedAccountAgeWitness.class.getSimpleName();
        return networkService.getDataService()
                .stream()
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedAccountAgeWitness.class::isInstance)
                                .map(AuthorizedAccountAgeWitness.class::cast)
                )
                .filter(e -> Arrays.equals(hash, e.getAccountAgeWitness().getHash()));
    }

}
