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
import bisq.common.encoding.Hex;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.common.util.ByteArrayUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
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
                    if (shouldRepublish(authorizedAccountTimestamp)) {
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

    public ReadOnlyObservableMap<ByteArray, AccountTimestamp> getAccountTimestampByHash() {
        return accountTimestampByHash;
    }

    public Optional<Long> findAccountTimestampDate(Account<?, ?> account) {
        byte[] hash = createHash(account);
        AccountTimestamp accountTimestamp = accountTimestampByHash.get(new ByteArray(hash));
        return Optional.ofNullable(accountTimestamp).map(AccountTimestamp::getDate);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void handleAuthorizedAccountTimestampAdded(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        AccountTimestamp accountTimestamp = authorizedAccountTimestamp.getAccountTimestamp();
        ByteArray accountTimestampHash = new ByteArray(accountTimestamp.getHash());
        AccountTimestamp existing = accountTimestampByHash.get(accountTimestampHash);
        if (existing != null && existing.getDate() != accountTimestamp.getDate()) {
            log.warn("Our existing accountTimestamp date is different to the date of the newly added. " +
                    "This should never happen. existing={} , authorizedAccountTimestamp={}", existing, authorizedAccountTimestamp);
        }
        accountTimestampByHash.putIfAbsent(accountTimestampHash, accountTimestamp);
    }

    private void handleAuthorizedAccountTimestampRemoved(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        AccountTimestamp accountTimestamp = authorizedAccountTimestamp.getAccountTimestamp();
        ByteArray accountTimestampHash = new ByteArray(accountTimestamp.getHash());
        accountTimestampByHash.remove(accountTimestampHash);
    }

    private void sendAccountTimestampRequest(Account<?, ?> account, AccountTimestamp accountTimestamp) {
        var selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            log.warn("No selected user identity. Skipping account timestamp request.");
            return;
        }

        KeyPair keyPair = account.getKeyPair();
        byte[] publicKeyEncoded = keyPair.getPublic().getEncoded();
        AccountPayload<?> accountPayload = account.getAccountPayload();
        long date = accountTimestamp.getDate();
        byte[] hash = accountTimestamp.getHash();
        byte[] salt = accountPayload.getSalt();
        KeyType keyType = account.getKeyType();
        String signatureAlgorithm = keyType.getSignatureAlgorithm();
        try {
            if (account.getAccountOrigin() == AccountOrigin.BISQ1_IMPORTED) {
                checkArgument(KeyType.DSA == keyType,
                        "KeyType must be DSA for imported accounts");
                // To ensure compatibility with Bisq 1 imported data, we use the byte arrays
                // This has weaknesses to reveal the salt as it has a fixed length
                byte[] fingerprint = accountPayload.getBisq1CompatibleFingerprint();
                byte[] saltedFingerprint = ByteArrayUtils.concat(fingerprint, salt);
                AuthorizeAccountTimestampV1Payload payload = new AuthorizeAccountTimestampV1Payload(
                        date,
                        hash,
                        fingerprint,
                        saltedFingerprint,
                        publicKeyEncoded
                );

                byte[] message = payload.toProto(true).toByteArray();
                byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), signatureAlgorithm);
                AuthorizeAccountTimestampV1Request request = new AuthorizeAccountTimestampV1Request(payload, signature);
                sendAuthorizeAccountTimestampRequest(request, selectedUserIdentity);
            } else {
                checkArgument(KeyType.EC == keyType,
                        "KeyType must be EC for new accounts created in Bisq2.");
                // We use 20 byte hashes instead of the byte arrays
                byte[] fingerprintHash = accountPayload.getBisq2FingerprintHash();
                byte[] saltedFingerprintHash = DigestUtil.hash(ByteArrayUtils.concat(fingerprintHash, salt));
                AuthorizeAccountTimestampV2Payload payload = new AuthorizeAccountTimestampV2Payload(
                        date,
                        hash,
                        fingerprintHash,
                        saltedFingerprintHash,
                        publicKeyEncoded
                );

                byte[] message = payload.toProto(true).toByteArray();
                byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate(), signatureAlgorithm);
                AuthorizeAccountTimestampV2Request request = new AuthorizeAccountTimestampV2Request(payload, signature);
                sendAuthorizeAccountTimestampRequest(request, selectedUserIdentity);
            }
        } catch (Exception e) {
            log.error("Failed to send accountTimestamp request", e);
            throw new RuntimeException(e);
        }
    }

    private void sendAuthorizeAccountTimestampRequest(EnvelopePayloadMessage request,
                                                      UserIdentity selectedUserIdentity) {
        authorizedBondedRolesService.getAuthorizedOracleNodes()
                .forEach(oracleNode ->
                        networkService.confidentialSend(request,
                                oracleNode.getNetworkId(),
                                selectedUserIdentity.getNetworkIdWithKeyPair()));
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

    private static byte[] createHash(Account<? extends PaymentMethod<?>, ?> account) {
        KeyPair keyPair = account.getKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        AccountPayload<?> accountPayload = account.getAccountPayload();

        byte[] salt = accountPayload.getSalt();

        if (account.getAccountOrigin() == AccountOrigin.BISQ1_IMPORTED) {
            byte[] fingerprint = accountPayload.getBisq1CompatibleFingerprint();
            byte[] saltedFingerprint = ByteArrayUtils.concat(fingerprint, salt);
            byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKey);
            byte[] hash = DigestUtil.hash(preimage);
            log.debug("createHashV1:\npublicKey={}\n" +
                            "salt={}\n" +
                            "fingerprint={}\n" +
                            "saltedFingerprint={}\n" +
                            "preimage={}\n" +
                            "hash={}",
                    Hex.encode(publicKey), Hex.encode(salt),
                    Hex.encode(fingerprint), Hex.encode(saltedFingerprint),
                    Hex.encode(preimage), Hex.encode(hash));
            return hash;
        } else {
            byte[] fingerprintHash = accountPayload.getBisq2FingerprintHash();
            byte[] saltedFingerprintHash = DigestUtil.hash(ByteArrayUtils.concat(fingerprintHash, salt));
            byte[] preimage = ByteArrayUtils.concat(saltedFingerprintHash, publicKey);
            byte[] hash = DigestUtil.hash(preimage);
            log.debug("createHashV2:\npublicKey={}\n" +
                            "salt={}\n" +
                            "fingerprintHash={}\n" +
                            "saltedFingerprintHash={}\n" +
                            "preimage={}\n" +
                            "hash={}",
                    Hex.encode(publicKey), Hex.encode(salt),
                    Hex.encode(fingerprintHash), Hex.encode(saltedFingerprintHash),
                    Hex.encode(preimage), Hex.encode(hash));
            return hash;
        }
    }

    public static void verifyHashV1(AuthorizeAccountTimestampV1Payload payload) {
        verifyHash(payload.getSaltedFingerprint(), payload.getPublicKey(), payload.getHash());
    }

    public static void verifyHashV2(AuthorizeAccountTimestampV2Payload payload) {
        verifyHash(payload.getSaltedFingerprintHash(), payload.getPublicKey(), payload.getHash());
    }

    public static void verifyHash(byte[] saltedFingerprint,
                                  byte[] publicKey,
                                  byte[] payloadHash) {
        byte[] preimage = ByteArrayUtils.concat(saltedFingerprint, publicKey);
        byte[] hash = DigestUtil.hash(preimage);

        checkArgument(Arrays.equals(payloadHash, hash),
                "AccountTimestamp hash is not matching the hash from the calculated preimage");
    }

    public static void verifySignatureV1(AuthorizeAccountTimestampV1Request request) throws GeneralSecurityException {
        AuthorizeAccountTimestampV1Payload payload = request.getPayload();
        KeyType keyType = KeyType.DSA;
        PublicKey publicKey = KeyGeneration.generatePublic(payload.getPublicKey(), keyType.getKeyAlgorithm());
        byte[] message = payload.toProto(true).toByteArray();
        verifySignature(message,
                publicKey,
                request.getSignature(),
                keyType.getSignatureAlgorithm());
    }

    public static void verifySignatureV2(AuthorizeAccountTimestampV2Request request) throws GeneralSecurityException {
        AuthorizeAccountTimestampV2Payload payload = request.getPayload();
        KeyType keyType = KeyType.EC;
        PublicKey publicKey = KeyGeneration.generatePublic(payload.getPublicKey(), keyType.getKeyAlgorithm());
        byte[] message = payload.toProto(true).toByteArray();
        verifySignature(message,
                publicKey,
                request.getSignature(),
                keyType.getSignatureAlgorithm());
    }

    private static void verifySignature(byte[] message,
                                        PublicKey publicKey,
                                        byte[] signature,
                                        String signatureAlgorithm) throws GeneralSecurityException {
        boolean isValid = SignatureUtil.verify(message,
                signature,
                publicKey,
                signatureAlgorithm);
        checkArgument(isValid, "Signature verification failed");
    }

    private static boolean shouldRepublish(AuthorizedAccountTimestamp authorizedAccountTimestamp) {
        long ttl = authorizedAccountTimestamp.getMetaData().getTtl();
        long halfExpiryDate = System.currentTimeMillis() - ttl / 2;
        return authorizedAccountTimestamp.getPublishDate() <= halfExpiryDate;
    }
}
