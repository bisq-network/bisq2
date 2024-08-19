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

package bisq.user.identity;

import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.security.*;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
public final class UserIdentityStore implements PersistableStore<UserIdentityStore> {
    // For plain text those data are set. If encryption is used the protobuf lists are not filled.
    private final Observable<UserIdentity> selectedUserIdentityObservable = new Observable<>();
    private final ObservableSet<UserIdentity> userIdentities = new ObservableSet<>();

    // For encrypted data we set those fields
    private Optional<EncryptedData> encryptedData = Optional.empty();
    private Optional<ScryptParameters> scryptParameters = Optional.empty();

    private transient Optional<AesSecretKey> aesSecretKey = Optional.empty();

    public UserIdentityStore() {
    }

    private UserIdentityStore(String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities) {
        this.userIdentities.setAll(userIdentities);
        setSelectedUserIdentityId(selectedUserIdentityId);
    }

    private UserIdentityStore(EncryptedData encryptedData,
                              ScryptParameters scryptParameters) {
        this.encryptedData = Optional.of(encryptedData);
        this.scryptParameters = Optional.of(scryptParameters);
    }

    private UserIdentityStore(String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities,
                              Optional<EncryptedData> encryptedData,
                              Optional<ScryptParameters> scryptParameters,
                              Optional<AesSecretKey> aesSecretKey) {
        this.userIdentities.setAll(userIdentities);
        setSelectedUserIdentityId(selectedUserIdentityId);

        this.encryptedData = encryptedData;
        this.scryptParameters = scryptParameters;
        this.aesSecretKey = aesSecretKey;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Proto
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public bisq.user.protobuf.UserIdentityStore.Builder getBuilder(boolean serializeForHash) {
        if (aesSecretKey.isPresent()) {
            long ts = System.currentTimeMillis();
            // We put the data we want to encrypt into a protobuf object.
            encryptedData = Optional.of(encryptPlainTextProto(getPlainTextBuilder().build()));

            log.info("Encryption at toProto took {} ms", System.currentTimeMillis() - ts);
            checkArgument(scryptParameters.isPresent());
            return bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .setEncryptedData(encryptedData.get().toProto(serializeForHash))
                    .setScryptParameters(scryptParameters.get().toProto(serializeForHash));
        } else {
            return getPlainTextBuilder();
        }
    }

    private bisq.user.protobuf.UserIdentityStore.Builder getPlainTextBuilder() {
        var builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                .addAllUserIdentities(userIdentities.stream()
                        .map(userIdentity -> userIdentity.toProto(false))
                        .collect(Collectors.toSet()));
        Optional.ofNullable(getSelectedUserIdentityId()).ifPresent(builder::setSelectedUserIdentityId);
        return builder;
    }

    @Override
    public bisq.user.protobuf.UserIdentityStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static UserIdentityStore fromProto(bisq.user.protobuf.UserIdentityStore proto) {
        if (proto.hasEncryptedData() && proto.hasScryptParameters()) {
            checkArgument(!proto.hasSelectedUserIdentityId());
            checkArgument(proto.getUserIdentitiesList().isEmpty());
            EncryptedData encryptedData = EncryptedData.fromProto(proto.getEncryptedData());
            ScryptParameters scryptParameters = ScryptParameters.fromProto(proto.getScryptParameters());
            return new UserIdentityStore(encryptedData, scryptParameters);
        } else {
            checkArgument(!proto.hasEncryptedData());
            checkArgument(!proto.hasScryptParameters());
            String selectedUserIdentityId = proto.hasSelectedUserIdentityId() ? proto.getSelectedUserIdentityId() : null;
            Set<UserIdentity> userIdentitySet = proto.getUserIdentitiesList().stream()
                    .map(UserIdentity::fromProto)
                    .collect(Collectors.toSet());
            return new UserIdentityStore(selectedUserIdentityId, userIdentitySet);
        }
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.UserIdentityStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PersistableStore
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public UserIdentityStore getClone() {
        return new UserIdentityStore(getSelectedUserIdentityId(),
                new HashSet<>(userIdentities),
                encryptedData,
                scryptParameters,
                aesSecretKey);
    }

    @Override
    public void applyPersisted(UserIdentityStore persisted) {
        Set<UserIdentity> persistedUserIdentities = persisted.getUserIdentities().stream()
                .map(userIdentity -> {
                    // We update ApplicationVersion and version at our own user profiles
                    UserProfile existingUserProfile = userIdentity.getUserProfile();
                    UserProfile userProfile = UserProfile.createNew(existingUserProfile.getNickName(),
                            existingUserProfile.getProofOfWork(),
                            existingUserProfile.getAvatarVersion(),
                            existingUserProfile.getNetworkId(),
                            existingUserProfile.getTerms(),
                            existingUserProfile.getStatement());
                    return new UserIdentity(userIdentity.getIdentity(), userProfile);
                }).collect(Collectors.toSet());
        userIdentities.setAll(persistedUserIdentities);
        setSelectedUserIdentityId(persisted.getSelectedUserIdentityId());

        encryptedData = persisted.getEncryptedData();
        scryptParameters = persisted.scryptParameters;

        Optional<AesSecretKey> persistedOptionalKey = persisted.aesSecretKey;
        if (persistedOptionalKey.isPresent()) {
            AesSecretKey clone = AesSecretKey.getClone(persistedOptionalKey.get());
            setAesSecretKey(clone);
            persistedOptionalKey.get().clear();
        } else {
            deleteAesSecretKey();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package scope API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<AesSecretKey> deriveKeyFromPassword(CharSequence password) {
        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            ScryptKeyDeriver scryptKeyDeriver;
            if (scryptParameters.isPresent()) {
                scryptKeyDeriver = new ScryptKeyDeriver(scryptParameters.get());
            } else {
                scryptKeyDeriver = new ScryptKeyDeriver();
                scryptParameters = Optional.of(scryptKeyDeriver.getScryptParameters());
            }
            try {
                AesSecretKey keyFromPassword = scryptKeyDeriver.deriveKeyFromPassword(password);
                setAesSecretKey(keyFromPassword);
                log.info("Deriving aesKey with scrypt took {} ms", System.currentTimeMillis() - ts);
                return keyFromPassword;
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        });
    }

    CompletableFuture<EncryptedData> encrypt() {
        checkArgument(aesSecretKey.isPresent(), "aesSecretKey must be present at encrypt.");
        checkArgument(scryptParameters.isPresent(), "scryptParameters must be present at encrypt.");
        long ts = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            EncryptedData encryptedData = encryptPlainTextProto(getPlainTextBuilder().build());
            log.info("encrypt took {} ms", System.currentTimeMillis() - ts);
            return encryptedData;
        }).whenComplete((encrypted, throwable) -> {
            if (throwable == null && encrypted != null) {
                this.encryptedData = Optional.of(encrypted);
            }
        });
    }

    CompletableFuture<Void> decrypt(AesSecretKey aesSecretKey) {
        checkArgument(encryptedData.isPresent(), "encryptedData must be present at decrypt.");
        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            try {
                byte[] decryptedData = AesGcm.decrypt(aesSecretKey, encryptedData.get().getIv(), encryptedData.get().getCipherText());
                Any any = ProtobufUtils.toAny(decryptedData);
                UserIdentityStore decrypted = fromProto(any.unpack(bisq.user.protobuf.UserIdentityStore.class));
                userIdentities.clear();
                userIdentities.addAll(decrypted.getUserIdentities());
                setSelectedUserIdentityId(decrypted.getSelectedUserIdentityId());
                log.info("decrypt took {} ms", System.currentTimeMillis() - ts);
                return null;
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    CompletableFuture<Void> removeKey(CharSequence password) {
        checkArgument(aesSecretKey.isPresent(), "aesSecretKey must be present at removeKey.");
        checkArgument(scryptParameters.isPresent(), "scryptParameters must be present at removeKey.");

        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            ScryptKeyDeriver scryptKeyDeriver = new ScryptKeyDeriver(scryptParameters.get());
            try {
                AesSecretKey keyFromPassword = scryptKeyDeriver.deriveKeyFromPassword(password);
                checkArgument(keyFromPassword.equals(aesSecretKey.get()),
                        "Provided password does not match our aesKey.");
                scryptParameters = Optional.empty();
                deleteAesSecretKey();
                log.info("removePassword took {} ms", System.currentTimeMillis() - ts);
                return null;
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void clearEncryptedData() {
        encryptedData = Optional.empty();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package scope Getter/Setter
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    Observable<UserIdentity> getSelectedUserIdentityObservable() {
        return selectedUserIdentityObservable;
    }

    UserIdentity getSelectedUserIdentity() {
        return selectedUserIdentityObservable.get();
    }

    ObservableSet<UserIdentity> getUserIdentities() {
        return userIdentities;
    }

    // We want to ensure that the selected object is the same as the one in the userIdentities set.
    void setSelectedUserIdentity(UserIdentity userIdentity) {
        if (userIdentities.isEmpty()) {
            log.error("userIdentities must not be empty. userIdentity={}", userIdentity);
            return;
        }

        Optional<UserIdentity> optionalUserIdentity = userIdentities.stream()
                .filter(e -> e.equals(userIdentity))
                .findAny();
        if (optionalUserIdentity.isPresent()) {
            selectedUserIdentityObservable.set(optionalUserIdentity.get());
        } else {
            log.warn("Could not find user identity in userIdentities.\n" +
                            "userIdentity={}\n" +
                            "userIdentities={}",
                    userIdentity, userIdentities);
            if (selectedUserIdentityObservable.get() == null) {
                log.warn("As selectedUserIdentity is null we select the fist found userIdentity from userIdentities.");
                selectedUserIdentityObservable.set(userIdentities.stream().findFirst().orElseThrow());
            } else {
                log.warn("As selectedUserIdentity is not null we ignore the call.");
            }
        }
    }

    Optional<EncryptedData> getEncryptedData() {
        return encryptedData;
    }

    Optional<AesSecretKey> getAESSecretKey() {
        return aesSecretKey;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private String getSelectedUserIdentityId() {
        return getSelectedUserIdentity() != null ? getSelectedUserIdentity().getId() : null;
    }

    private void setSelectedUserIdentityId(String selectedUserIdentityId) {
        if (userIdentities.isEmpty()) {
            log.error("userIdentities must not be empty. userIdentity={}", selectedUserIdentityId);
            return;
        }
        Optional<UserIdentity> optionalUserIdentity = userIdentities.stream()
                .filter(e -> e.getId().equals(selectedUserIdentityId))
                .findAny();
        if (optionalUserIdentity.isPresent()) {
            selectedUserIdentityObservable.set(optionalUserIdentity.get());
        } else {
            log.warn("Could not find user identity in userIdentities.\n" +
                            "selectedUserIdentityId={}\n" +
                            "userIdentities={}",
                    selectedUserIdentityId, userIdentities);
            if (selectedUserIdentityObservable.get() == null) {
                log.warn("As selectedUserIdentity is null we select the fist found userIdentity from userIdentities.");
                selectedUserIdentityObservable.set(userIdentities.stream().findFirst().orElseThrow());
            } else {
                log.warn("As selectedUserIdentity is not null we ignore the call.");
            }
        }
    }

    private void deleteAesSecretKey() {
        setAesSecretKey(Optional.empty());
    }

    private void setAesSecretKey(AesSecretKey aesSecretKey) {
        setAesSecretKey(Optional.of(aesSecretKey));
    }

    private void setAesSecretKey(Optional<AesSecretKey> aesSecretKey) {
        this.aesSecretKey.ifPresent(AesSecretKey::clear);
        this.aesSecretKey = aesSecretKey;
    }

    private EncryptedData encryptPlainTextProto(bisq.user.protobuf.UserIdentityStore plainTextProto) {
        try {
            byte[] plainText = Any.pack(plainTextProto).toByteArray();
            byte[] iv = AesGcm.generateIv().getIV();
            byte[] cipherText = AesGcm.encrypt(aesSecretKey.orElseThrow(), iv, plainText);
            return new EncryptedData(iv, cipherText);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}