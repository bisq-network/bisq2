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

package bisq.account.timestamp;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
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
import java.util.stream.Stream;

@Slf4j
public class AccountTimestampService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    @Getter
    private final ObservableHashMap<ByteArray, AccountTimestamp> accountAgeWitnessByHash = new ObservableHashMap<>();

    public AccountTimestampService(NetworkService networkService,
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
        String storageKey = AuthorizedAccountTimestamp.class.getSimpleName();
        networkService.getDataService()
                .stream() // turns Optional<DataService> into Stream<DataService>
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedAccountTimestamp.class::isInstance)
                                .map(AuthorizedAccountTimestamp.class::cast)
                ).map(e -> e)
                .forEach(this::handleAuthorizedAccountAgeWitnessAdded);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountTimestamp authorizedAccountTimestamp) {
            handleAuthorizedAccountAgeWitnessAdded(authorizedAccountTimestamp);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedAccountTimestamp authorizedAccountTimestamp) {
            handleAuthorizedAccountAgeWitnessRemoved(authorizedAccountTimestamp);
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

    private void registerAccountAgeWitness(Account<?, ?> account) {
        long now = System.currentTimeMillis();
        KeyPair keyPair = account.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        KeyAlgorithm keyAlgorithm = account.getKeyAlgorithm();
        AccountPayload<?> accountPayload = account.getAccountPayload();

        var selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            log.warn("No selected user identity. Skipping account timestamp registration.");
            return;
        }

        byte[] saltedFingerprint = getSaltedFingerprint(accountPayload);
        byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKeyEncoded);
        byte[] hash = DigestUtil.hash(preimage);

        AccountTimestamp accountTimestamp = new AccountTimestamp(hash, System.currentTimeMillis());
        byte[] message = accountTimestamp.toProto(true).toByteArray();
        try {
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), account.getSignatureAlgorithm());
            TimestampType timestampType = account.getAccountOrigin() == AccountOrigin.BISQ1_IMPORTED
                    ? TimestampType.BISQ1_IMPORTED
                    : TimestampType.BISQ2_NEW;
            AuthorizeAccountTimestampRequest authorizeAccountAgeWitnessRequest = new AuthorizeAccountTimestampRequest(timestampType,
                    accountTimestamp,
                    saltedFingerprint,
                    publicKeyEncoded,
                    signature,
                    keyAlgorithm,
                    now);

            authorizedBondedRolesService.getAuthorizedOracleNodes()
                    .forEach(oracleNode ->
                            networkService.confidentialSend(authorizeAccountAgeWitnessRequest,
                                    oracleNode.getNetworkId(),
                                    selectedUserIdentity.getNetworkIdWithKeyPair()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

  /*  public Result<Void> verifyAccountAgeWitness(AccountTimestamp accountTimestamp,
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
            byte[] publicKeyEncoded = publicKey.getEncoded();

            byte[] saltedFingerprint = getSaltedFingerprint(accountPayload);
            byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKeyEncoded);
            byte[] hash = DigestUtil.hash(preimage);

            checkArgument(Arrays.equals(accountTimestamp.getHash(), hash),
                    "AgeWitnessHash not matching hashFromAccountPayload");
            checkArgument(SignatureUtil.verify(message, signature, publicKey, keyAlgorithm.getAlgorithm()), "Signature verification failed");
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
*/


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void handleAuthorizedAccountAgeWitnessAdded(AuthorizedAccountTimestamp authorizedAccountAgeWitness) {
        accountAgeWitnessByHash.putIfAbsent(getAccountAgeWitnessHash(authorizedAccountAgeWitness),
                authorizedAccountAgeWitness.getAccountTimestamp());
    }

    private void handleAuthorizedAccountAgeWitnessRemoved(AuthorizedAccountTimestamp authorizedAccountAgeWitness) {
        accountAgeWitnessByHash.remove(getAccountAgeWitnessHash(authorizedAccountAgeWitness));
    }

    private static ByteArray getAccountAgeWitnessHash(AuthorizedAccountTimestamp authorizedAccountAgeWitness) {
        return new ByteArray(authorizedAccountAgeWitness.getAccountTimestamp().getHash());
    }


    private boolean requireRegistration(Account<? extends PaymentMethod<?>, ?> account) {
        byte[] hash = createHash(account);
        return findAuthorizedAccountAgeWitness(hash)
                .map(AccountTimestampService::isHalfExpired)
                .findAny()
                .orElse(true);
    }

    private static byte[] getSaltedFingerprint(AccountPayload<?> accountPayload) {
        byte[] salt = accountPayload.getSalt();
        byte[] preimage = accountPayload.getFingerprint();
        return ByteArrayUtils.concat(preimage, salt);
    }

    public Optional<AccountTimestamp> findAccountAgeWitness(ByteArray hash) {
        return Optional.ofNullable(accountAgeWitnessByHash.get(hash));
    }

    private byte[] createHash(Account<? extends PaymentMethod<?>, ?> account) {
        AccountPayload<?> accountPayload = account.getAccountPayload();
        byte[] salt = accountPayload.getSalt();
        byte[] ageWitnessInputData = accountPayload.getFingerprint();
        byte[] blindedAgeWitnessInputData = ByteArrayUtils.concat(ageWitnessInputData, salt);
        KeyPair keyPair = account.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] preimage = ByteArrayUtils.concat(blindedAgeWitnessInputData, publicKeyBytes);
        return DigestUtil.hash(preimage);
    }

    private static boolean isHalfExpired(AuthorizedAccountTimestamp authorizedAccountAgeWitness) {
        long ttl = authorizedAccountAgeWitness.getMetaData().getTtl();
        long halfExpiryDate = System.currentTimeMillis() - ttl / 2;
        return authorizedAccountAgeWitness.getPublishDate() <= halfExpiryDate;
    }

    private Stream<AuthorizedAccountTimestamp> findAuthorizedAccountAgeWitness(byte[] hash) {
        String storageKey = AuthorizedAccountTimestamp.class.getSimpleName();
        return networkService.getDataService()
                .stream()
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedAccountTimestamp.class::isInstance)
                                .map(AuthorizedAccountTimestamp.class::cast)
                )
                .filter(e -> Arrays.equals(hash, e.getAccountTimestamp().getHash()));
    }

}
