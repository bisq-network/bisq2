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
    private final Observable<UserIdentity> selectedUserIdentityObservable = new Observable<>();
    private final ObservableSet<UserIdentity> userIdentities = new ObservableSet<>();
    private Optional<EncryptedData> encryptedData = Optional.empty();
    private Optional<ScryptParameters> scryptParameters = Optional.empty();
    private transient Optional<AESSecretKey> aesSecretKey = Optional.empty();


    public UserIdentityStore() {
    }

    private UserIdentityStore(@Nullable String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities) {
        this.userIdentities.setAll(userIdentities);
        setSelectedUserIdentityId(selectedUserIdentityId);
    }

    private UserIdentityStore(EncryptedData encryptedData,
                              ScryptParameters scryptParameters) {
        this.encryptedData = Optional.of(encryptedData);
        this.scryptParameters = Optional.of(scryptParameters);
    }

    private UserIdentityStore(@Nullable String selectedUserIdentityId,
                              Set<UserIdentity> userIdentities,
                              Optional<EncryptedData> encryptedData,
                              Optional<ScryptParameters> scryptParameters,
                              Optional<AESSecretKey> aesSecretKey) {
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
    public bisq.user.protobuf.UserIdentityStore toProto() {
        bisq.user.protobuf.UserIdentityStore.Builder builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                .addAllUserIdentities(userIdentities.stream().map(UserIdentity::toProto).collect(Collectors.toSet()));
        Optional.ofNullable(getSelectedUserIdentityId()).ifPresent(builder::setSelectedUserIdentityId);
        bisq.user.protobuf.UserIdentityStore plainTextProto = builder.build();
        if (aesSecretKey.isPresent()) {
            long ts = System.currentTimeMillis();
            encryptedData = Optional.of(encryptPlainTextProto(plainTextProto));
            log.info("Encryption at toProto took {} ms", System.currentTimeMillis() - ts);
            checkArgument(scryptParameters.isPresent());
            builder = bisq.user.protobuf.UserIdentityStore.newBuilder()
                    .setEncryptedData(encryptedData.get().toProto())
                    .setScryptParameters(scryptParameters.get().toProto());
            return builder.build();
        } else {
            return plainTextProto;
        }
    }

    public static UserIdentityStore fromProto(bisq.user.protobuf.UserIdentityStore proto) {
        if (proto.hasEncryptedData() && proto.hasScryptParameters()) {
            checkArgument(proto.getUserIdentitiesList().isEmpty());
            EncryptedData encryptedData = EncryptedData.fromProto(proto.getEncryptedData());
            ScryptParameters scryptParameters = ScryptParameters.fromProto(proto.getScryptParameters());
            return new UserIdentityStore(encryptedData, scryptParameters);
        } else {
            checkArgument(!proto.hasEncryptedData());
            checkArgument(!proto.hasScryptParameters());
            return new UserIdentityStore(proto.hasSelectedUserIdentityId() ? proto.getSelectedUserIdentityId() : null,
                    proto.getUserIdentitiesList().stream()
                            .map(UserIdentity::fromProto)
                            .collect(Collectors.toSet()));
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
        return new UserIdentityStore(getSelectedUserIdentityId(), userIdentities, encryptedData, scryptParameters, aesSecretKey);
    }

    @Override
    public void applyPersisted(UserIdentityStore persisted) {
        userIdentities.setAll(persisted.getUserIdentities());
        setSelectedUserIdentityId(persisted.getSelectedUserIdentityId());

        encryptedData = persisted.getEncryptedData();
        scryptParameters = persisted.scryptParameters;

        Optional<AESSecretKey> persistedOptionalKey = persisted.aesSecretKey;
        if (persistedOptionalKey.isPresent()) {
            AESSecretKey clone = AESSecretKey.getClone(persistedOptionalKey.get());
            setAesSecretKey(clone);
            persistedOptionalKey.get().clear();
        } else {
            deleteAesSecretKey();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package scope API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<AESSecretKey> deriveKeyFromPassword(String password) {
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
                AESSecretKey keyFromPassword = scryptKeyDeriver.deriveKeyFromPassword(password);
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

    CompletableFuture<Void> decrypt(AESSecretKey aesSecretKey) {
        checkArgument(encryptedData.isPresent(), "encryptedData must be present at decrypt.");
        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            try {
                byte[] decryptedData = AESEncryption.decrypt(encryptedData.get(), aesSecretKey);
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

    CompletableFuture<Void> removeKey(String password) {
        checkArgument(aesSecretKey.isPresent(), "aesSecretKey must be present at removeKey.");
        checkArgument(scryptParameters.isPresent(), "scryptParameters must be present at removeKey.");

        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            ScryptKeyDeriver scryptKeyDeriver = new ScryptKeyDeriver(scryptParameters.get());
            try {
                AESSecretKey keyFromPassword = scryptKeyDeriver.deriveKeyFromPassword(password);
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

    Optional<AESSecretKey> getAESSecretKey() {
        return aesSecretKey;
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

    private void setAesSecretKey(AESSecretKey aesSecretKey) {
        setAesSecretKey(Optional.of(aesSecretKey));
    }

    private void setAesSecretKey(Optional<AESSecretKey> aesSecretKey) {
        this.aesSecretKey.ifPresent(AESSecretKey::clear);
        this.aesSecretKey = aesSecretKey;
    }

    private EncryptedData encryptPlainTextProto(bisq.user.protobuf.UserIdentityStore plainTextProto) {
        try {
            byte[] plainTextProtoAsBytes = ProtobufUtils.getByteArrayFromProto(Any.pack(plainTextProto));
            return AESEncryption.encrypt(plainTextProtoAsBytes, aesSecretKey.orElseThrow());
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}