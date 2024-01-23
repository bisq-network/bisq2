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
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;
import bisq.persistence.PersistableStore;
import bisq.security.*;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

    // lastUserProfilePublishingDate will be stored in both cases unencrypted.
    private long lastUserProfilePublishingDate;

    private transient Optional<AesSecretKey> aesSecretKey = Optional.empty();

    public UserIdentityStore() {
    }

    private UserIdentityStore(@Nullable String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities,
                              long lastUserProfilePublishingDate) {
        this.userIdentities.setAll(userIdentities);
        setSelectedUserIdentityId(selectedUserIdentityId);
        this.lastUserProfilePublishingDate = lastUserProfilePublishingDate;
    }

    private UserIdentityStore(EncryptedData encryptedData,
                              ScryptParameters scryptParameters,
                              long lastUserProfilePublishingDate) {
        this.encryptedData = Optional.of(encryptedData);
        this.scryptParameters = Optional.of(scryptParameters);
        this.lastUserProfilePublishingDate = lastUserProfilePublishingDate;
    }

    private UserIdentityStore(@Nullable String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities,
                              Optional<EncryptedData> encryptedData,
                              Optional<ScryptParameters> scryptParameters,
                              Optional<AesSecretKey> aesSecretKey,
                              long lastUserProfilePublishingDate) {
        this.userIdentities.setAll(userIdentities);
        setSelectedUserIdentityId(selectedUserIdentityId);

        this.encryptedData = encryptedData;
        this.scryptParameters = scryptParameters;
        this.aesSecretKey = aesSecretKey;
        this.lastUserProfilePublishingDate = lastUserProfilePublishingDate;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Proto
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public bisq.user.protobuf.UserIdentityStore toProto() {
        if (aesSecretKey.isPresent()) {
            long ts = System.currentTimeMillis();
            // We put the data we want to encrypt into a protobuf object.
            bisq.user.protobuf.UserIdentityStore.Builder plainTextProtoBuilder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()));
            Optional.ofNullable(getSelectedUserIdentityId()).ifPresent(plainTextProtoBuilder::setSelectedUserIdentityId);
            bisq.user.protobuf.UserIdentityStore plainTextProto = plainTextProtoBuilder.build();
            encryptedData = Optional.of(encryptPlainTextProto(plainTextProto));

            log.info("Encryption at toProto took {} ms", System.currentTimeMillis() - ts);
            checkArgument(scryptParameters.isPresent());
            bisq.user.protobuf.UserIdentityStore.Builder builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .setEncryptedData(encryptedData.get().toProto())
                    .setScryptParameters(scryptParameters.get().toProto())
                    .setLastUserProfilePublishingDate(lastUserProfilePublishingDate);
            return builder.build();
        } else {
            bisq.user.protobuf.UserIdentityStore.Builder builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()));
            Optional.ofNullable(getSelectedUserIdentityId()).ifPresent(builder::setSelectedUserIdentityId);
            return builder
                    .setLastUserProfilePublishingDate(lastUserProfilePublishingDate)
                    .build();
        }
    }

    public static UserIdentityStore fromProto(bisq.user.protobuf.UserIdentityStore proto) {
        long lastUserProfilePublishingDate = proto.getLastUserProfilePublishingDate();
        if (proto.hasEncryptedData() && proto.hasScryptParameters()) {
            checkArgument(!proto.hasSelectedUserIdentityId());
            checkArgument(proto.getUserIdentitiesList().isEmpty());
            EncryptedData encryptedData = EncryptedData.fromProto(proto.getEncryptedData());
            ScryptParameters scryptParameters = ScryptParameters.fromProto(proto.getScryptParameters());
            return new UserIdentityStore(encryptedData, scryptParameters, lastUserProfilePublishingDate);
        } else {
            checkArgument(!proto.hasEncryptedData());
            checkArgument(!proto.hasScryptParameters());
            String selectedUserIdentityId = proto.hasSelectedUserIdentityId() ? proto.getSelectedUserIdentityId() : null;
            Set<UserIdentity> userIdentitySet = proto.getUserIdentitiesList().stream()
                    .map(UserIdentity::fromProto)
                    .collect(Collectors.toSet());
            return new UserIdentityStore(selectedUserIdentityId,
                    userIdentitySet,
                    lastUserProfilePublishingDate);
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
                userIdentities,
                encryptedData,
                scryptParameters,
                aesSecretKey,
                lastUserProfilePublishingDate);
    }

    @Override
    public void applyPersisted(UserIdentityStore persisted) {
        userIdentities.setAll(persisted.getUserIdentities());
        setSelectedUserIdentityId(persisted.getSelectedUserIdentityId());

        lastUserProfilePublishingDate = persisted.getLastUserProfilePublishingDate();
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
            bisq.user.protobuf.UserIdentityStore.Builder builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()));
            Optional.ofNullable(getSelectedUserIdentityId()).ifPresent(builder::setSelectedUserIdentityId);
            bisq.user.protobuf.UserIdentityStore plainTextProto = builder.build();
            EncryptedData encryptedData = encryptPlainTextProto(plainTextProto);
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

    @Nullable
    UserIdentity getSelectedUserIdentity() {
        return selectedUserIdentityObservable.get();
    }

    ObservableSet<UserIdentity> getUserIdentities() {
        return userIdentities;
    }

    // We want to ensure that the selected object is the same as the one in the userIdentities set.
    void setSelectedUserIdentity(@Nullable UserIdentity userIdentity) {
        selectedUserIdentityObservable.set(userIdentities.stream()
                .filter(e -> e.equals(userIdentity))
                .findAny()
                .orElse(null));
    }

    Optional<EncryptedData> getEncryptedData() {
        return encryptedData;
    }

    Optional<AesSecretKey> getAESSecretKey() {
        return aesSecretKey;
    }

    long getLastUserProfilePublishingDate() {
        return lastUserProfilePublishingDate;
    }

    void setLastUserProfilePublishingDate(long lastUserProfilePublishingDate) {
        this.lastUserProfilePublishingDate = lastUserProfilePublishingDate;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    private String getSelectedUserIdentityId() {
        return getSelectedUserIdentity() != null ? getSelectedUserIdentity().getId() : null;
    }

    private void setSelectedUserIdentityId(@Nullable String selectedUserIdentityId) {
        selectedUserIdentityObservable.set(userIdentities.stream()
                .filter(userIdentity -> userIdentity.getId().equals(selectedUserIdentityId))
                .findAny()
                .orElse(null));
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
            byte[] plainText = ProtobufUtils.getByteArrayFromProto(Any.pack(plainTextProto));
            byte[] iv = AesGcm.generateIv().getIV();
            byte[] cipherText = AesGcm.encrypt(aesSecretKey.orElseThrow(), iv, plainText);
            return new EncryptedData(iv, cipherText);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}