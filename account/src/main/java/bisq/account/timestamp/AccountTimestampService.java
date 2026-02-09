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
import bisq.common.data.Result;
import bisq.common.encoding.Hex;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.common.util.ByteArrayUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AccountTimestampService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    private final ObservableHashMap<ByteArray, AccountTimestamp> accountTimestampByHash = new ObservableHashMap<>();

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
                .stream()
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedAccountTimestamp.class::isInstance)
                                .map(AuthorizedAccountTimestamp.class::cast)
                )
                .forEach(this::handleAuthorizedAccountTimestampAdded);

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
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getDistributedData() instanceof AuthorizedAccountTimestamp authorizedAccountTimestamp) {
            handleAuthorizedAccountTimestampAdded(authorizedAccountTimestamp);
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getDistributedData() instanceof AuthorizedAccountTimestamp authorizedAccountTimestamp) {
            handleAuthorizedAccountTimestampRemoved(authorizedAccountTimestamp);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void handleAddedAccount(Account<?, ?> account) {
        byte[] hash = createHash(account);
        findAuthorizedAccountTimestamp(hash).findAny()
                .ifPresentOrElse(authorizedAccountTimestamp -> {
                    if (isHalfExpired(authorizedAccountTimestamp)) {
                        // Republish
                        sendAccountTimestampRequest(account, authorizedAccountTimestamp.getAccountTimestamp());
                    }
                }, () -> {
                    // New timestamp if it is a new account or republish with the account creation date in case the
                    // authorizedAccountTimestamp has already expired.
                    AccountTimestamp accountTimestamp = new AccountTimestamp(hash, account.getCreationDate());
                    sendAccountTimestampRequest(account, accountTimestamp);
                });
    }

    public static Result<Boolean> verifyAccountTimestamp(AccountTimestamp accountTimestamp,
                                                         AccountPayload<?> accountPayload,
                                                         PublicKey publicKey,
                                                         byte[] signature,
                                                         KeyAlgorithm keyAlgorithm) {
        try {
            byte[] fingerprint = accountPayload.getFingerprint();
            byte[] salt = accountPayload.getSalt();
            byte[] saltedFingerprint = ByteArrayUtils.concat(fingerprint, salt);
            verifyHash(saltedFingerprint, publicKey.getEncoded(), accountTimestamp);
            verifySignature(accountTimestamp,
                    publicKey,
                    signature,
                    keyAlgorithm);
            return Result.success(true);
        } catch (Exception e) {
            log.warn("verifyAccountTimestamp failed", e);
            return Result.failure(e);
        }
    }

    public ReadOnlyObservableMap<ByteArray, AccountTimestamp> getAccountTimestampByHash() {
        return accountTimestampByHash;
    }

    public Optional<Long> findAccountTimestamp(Account<?, ?> account) {
        byte[] hash = createHash(account);
        AccountTimestamp accountTimestamp = accountTimestampByHash.get(new ByteArray(hash));
        return Optional.ofNullable(accountTimestamp).map(AccountTimestamp::getDate);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void handleAuthorizedAccountTimestampAdded(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        AccountTimestamp accountTimestamp = authorizedAccountTimestamp.getAccountTimestamp();
        ByteArray accountTimestampHash = getAccountTimestampHash(accountTimestamp);
        accountTimestampByHash.putIfAbsent(accountTimestampHash, accountTimestamp);
    }

    private void handleAuthorizedAccountTimestampRemoved(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        accountTimestampByHash.remove(getAccountTimestampHash(authorizedAccountTimestamp.getAccountTimestamp()));
    }

    private void sendAccountTimestampRequest(Account<?, ?> account, AccountTimestamp accountTimestamp) {
        var selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            log.warn("No selected user identity. Skipping account timestamp request.");
            return;
        }

        KeyPair keyPair = account.getKeyPair();
        byte[] publicKeyEncoded = keyPair.getPublic().getEncoded();
        byte[] saltedFingerprint = getSaltedFingerprint(account.getAccountPayload());
        byte[] message = accountTimestamp.toProto(true).toByteArray();
        try {
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), account.getSignatureAlgorithm());
            TimestampType timestampType = account.getAccountOrigin() == AccountOrigin.BISQ1_IMPORTED
                    ? TimestampType.BISQ1_IMPORTED
                    : TimestampType.BISQ2_NEW;
            AuthorizeAccountTimestampRequest request = new AuthorizeAccountTimestampRequest(timestampType,
                    accountTimestamp,
                    saltedFingerprint,
                    publicKeyEncoded,
                    signature,
                    account.getKeyAlgorithm());

            authorizedBondedRolesService.getAuthorizedOracleNodes()
                    .forEach(oracleNode ->
                            networkService.confidentialSend(request,
                                    oracleNode.getNetworkId(),
                                    selectedUserIdentity.getNetworkIdWithKeyPair()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<AuthorizedAccountTimestamp> findAuthorizedAccountTimestamp(byte[] hash) {
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

    private static ByteArray getAccountTimestampHash(AccountTimestamp accountTimestamp) {
        return new ByteArray(accountTimestamp.getHash());
    }

    private static byte[] getSaltedFingerprint(AccountPayload<?> accountPayload) {
        byte[] salt = accountPayload.getSalt();
        byte[] preimage = accountPayload.getFingerprint();
        return ByteArrayUtils.concat(preimage, salt);
    }

    private static byte[] createHash(Account<? extends PaymentMethod<?>, ?> account) {
        KeyPair keyPair = account.getKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        AccountPayload<?> accountPayload = account.getAccountPayload();

        byte[] salt = accountPayload.getSalt();
        byte[] fingerprint = accountPayload.getFingerprint();
        byte[] saltedFingerprint = ByteArrayUtils.concat(fingerprint, salt);
        byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKeyBytes);
        byte[] hash = DigestUtil.hash(preimage);

        log.debug("createHash:\npublicKeyBytes={}\n" +
                        "salt={}\n" +
                        "fingerprint={}\n" +
                        "saltedFingerprint={}\n" +
                        "preimage={}\n" +
                        "hash={}", Hex.encode(publicKeyBytes), Hex.encode(salt),
                Hex.encode(fingerprint), Hex.encode(saltedFingerprint),
                Hex.encode(preimage), Hex.encode(hash));
        return hash;
    }

    public static void verifyHash(AuthorizeAccountTimestampRequest request) {
        verifyHash(request.getSaltedFingerprint(), request.getPublicKey(), request.getAccountTimestamp());
    }

    public static void verifyHash(byte[] saltedFingerprint, byte[] publicKeyBytes, AccountTimestamp accountTimestamp) {
        byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKeyBytes);
        byte[] hash = DigestUtil.hash(preimage);

        checkArgument(Arrays.equals(accountTimestamp.getHash(), hash),
                "AccountTimestamp hash is not matching the hash from the calculated preimage");
    }

    public static void verifySignature(AuthorizeAccountTimestampRequest request) throws GeneralSecurityException {
        KeyAlgorithm keyAlgorithm = request.getKeyAlgorithm();
        PublicKey publicKey = KeyGeneration.generatePublic(request.getPublicKey(), keyAlgorithm.getAlgorithm());
        verifySignature(request.getAccountTimestamp(),
                publicKey,
                request.getSignature(),
                keyAlgorithm);
    }

    public static void verifySignature(AccountTimestamp accountTimestamp,
                                       PublicKey publicKey,
                                       byte[] signature,
                                       KeyAlgorithm keyAlgorithm) throws GeneralSecurityException {
        byte[] message = accountTimestamp.toProto(true).toByteArray();
        boolean isValid = SignatureUtil.verify(message,
                signature,
                publicKey,
                Account.getSignatureAlgorithm(keyAlgorithm));
        checkArgument(isValid, "Signature verification failed");
    }

    private static boolean isHalfExpired(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        long ttl = authorizedAccountTimestamp.getMetaData().getTtl();
        long halfExpiryDate = System.currentTimeMillis() - ttl / 2;
        return authorizedAccountTimestamp.getPublishDate() <= halfExpiryDate;
    }

}
