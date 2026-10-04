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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rule 2 of docs/specifications/bonded-roles/authorized-data.md: if authorized data has the static flag, or if its type
 * requires a static key, its authorized key must be one of the static keys of its type. The store checks it when it
 * adds data (rule 4) and when it loads a store (rule 5).
 */
class StaticKeyRequirementTest {
    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    private KeyPair staticKeyPair;
    private KeyPair otherKeyPair;
    private Set<String> staticKeys;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestStaticKeyData.class, TestStaticKeyData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "staticKey",
                TestStaticKeyData.class.getSimpleName());
        staticKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        staticKeys = Set.of(Hex.encode(staticKeyPair.getPublic().getEncoded()));
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // The publisher sets the static flag, so the requirement of the type applies also without it. Nodes of earlier
    // versions relay such data, so the rejection is no severe failure.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void typeWhichRequiresAStaticKeyRejectsDataOfAnotherKey(boolean staticPublicKeysProvided)
            throws GeneralSecurityException {
        TestStaticKeyData data = TestStaticKeyData.requiringStaticKey("content", staticKeys, staticPublicKeysProvided);

        DataStorageResult result = store.add(createAddRequest(authorize(data, otherKeyPair), otherKeyPair));

        assertFalse(result.isSuccess());
        assertTrue(result.isAuthorizedKeyNotStatic(), result.toString());
        assertFalse(result.isSevereFailure(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void typeWhichRequiresAStaticKeyAcceptsDataOfAStaticKey(boolean staticPublicKeysProvided)
            throws GeneralSecurityException {
        TestStaticKeyData data = TestStaticKeyData.requiringStaticKey("content", staticKeys, staticPublicKeysProvided);

        DataStorageResult result = store.add(createAddRequest(authorize(data, staticKeyPair), staticKeyPair));

        assertTrue(result.isSuccess(), result.toString());
        assertEquals(1, store.getPersistableStore().getMap().size());
    }

    // Without the static flag, another key can authorize data with a bonded role, which the store does not know (rule 3)
    @Test
    void typeWithoutRequirementAcceptsDataOfAnotherKeyWithoutStaticFlag() throws GeneralSecurityException {
        TestStaticKeyData data = TestStaticKeyData.withoutRequirement("content", staticKeys, false);

        DataStorageResult result = store.add(createAddRequest(authorize(data, otherKeyPair), otherKeyPair));

        assertTrue(result.isSuccess(), result.toString());
    }

    @Test
    void staticFlagRequiresAStaticKey() throws GeneralSecurityException {
        TestStaticKeyData data = TestStaticKeyData.withoutRequirement("content", staticKeys, true);

        DataStorageResult result = store.add(createAddRequest(authorize(data, otherKeyPair), otherKeyPair));

        assertFalse(result.isSuccess());
        assertTrue(result.isAuthorizedKeyNotStatic(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }

    // The key is a lookup in a set. The store checks it before the authorization signature, whose verification costs
    // more. An invalid signature of a static key is a severe failure.
    @Test
    void keyIsCheckedBeforeTheAuthorizationSignature() throws GeneralSecurityException {
        TestStaticKeyData data = TestStaticKeyData.requiringStaticKey("content", staticKeys, false);
        AuthorizedData withInvalidSignatureOfOtherKey = withSignatureOfOtherData(data, otherKeyPair);
        AuthorizedData withInvalidSignatureOfStaticKey = withSignatureOfOtherData(data, staticKeyPair);
        assertTrue(withInvalidSignatureOfOtherKey.isAuthorizationSignatureInvalid());

        DataStorageResult resultOfOtherKey = store.add(createAddRequest(withInvalidSignatureOfOtherKey, otherKeyPair));
        DataStorageResult resultOfStaticKey = store.add(createAddRequest(withInvalidSignatureOfStaticKey,
                staticKeyPair));

        assertTrue(resultOfOtherKey.isAuthorizedKeyNotStatic(), resultOfOtherKey.toString());
        assertFalse(resultOfOtherKey.isSevereFailure(), resultOfOtherKey.toString());
        assertFalse(resultOfStaticKey.isSuccess());
        assertFalse(resultOfStaticKey.isAuthorizedKeyNotStatic(), resultOfStaticKey.toString());
        assertTrue(resultOfStaticKey.isSevereFailure(), resultOfStaticKey.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }

    // A node of an earlier version accepted data of another key for types which require a static key now, and this
    // version can remove a static key. When the store loads, it removes such data. It keeps the other data.
    @Test
    void storeRemovesDataOfKeysWhichItDoesNotAllowWhenItLoads() throws GeneralSecurityException {
        AddAuthenticatedDataRequest requiredByTypeOfOtherKey = createAddRequest(authorize(
                TestStaticKeyData.requiringStaticKey("required by type", staticKeys, false), otherKeyPair), otherKeyPair);
        AddAuthenticatedDataRequest staticFlagOfOtherKey = createAddRequest(authorize(
                TestStaticKeyData.withoutRequirement("static flag", staticKeys, true), otherKeyPair), otherKeyPair);
        AddAuthenticatedDataRequest requiredByTypeOfStaticKey = createAddRequest(authorize(
                TestStaticKeyData.requiringStaticKey("static key", staticKeys, false), staticKeyPair), staticKeyPair);
        AddAuthenticatedDataRequest notRequiredOfOtherKey = createAddRequest(authorize(
                TestStaticKeyData.withoutRequirement("not required", staticKeys, false), otherKeyPair), otherKeyPair);
        for (AddAuthenticatedDataRequest request : Set.of(requiredByTypeOfOtherKey, staticFlagOfOtherKey,
                requiredByTypeOfStaticKey, notRequiredOfOtherKey)) {
            store.getPersistableStore().getMap().put(request.getStorageKey(), request);
        }

        DataStore<AuthenticatedDataRequest> loaded = store.prunePersisted(
                store.getPersistableStore().getMutableClone());

        Set<ByteArray> loadedKeys = loaded.getMap().keySet();
        assertEquals(Set.of(requiredByTypeOfStaticKey.getStorageKey(), notRequiredOfOtherKey.getStorageKey()),
                loadedKeys);
    }

    private AddAuthenticatedDataRequest createAddRequest(AuthorizedData authorizedData, KeyPair publisherKeyPair)
            throws GeneralSecurityException {
        return AddAuthenticatedDataRequest.from(store, authorizedData, publisherKeyPair);
    }

    private static AuthorizedData authorize(TestStaticKeyData data, KeyPair keyPair) throws GeneralSecurityException {
        byte[] signature = SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate());
        return new AuthorizedData(data, Optional.of(signature), keyPair.getPublic());
    }

    private static AuthorizedData withSignatureOfOtherData(TestStaticKeyData data, KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] signature = SignatureUtil.sign(new byte[]{1, 2, 3}, keyPair.getPrivate());
        return new AuthorizedData(data, Optional.of(signature), keyPair.getPublic());
    }

    // Authorized data which names no owner, so its authorized key owns it. A real type has its static keys and its
    // requirement in the code. The tests pass them, because they use generated key pairs.
    private static final class TestStaticKeyData implements AuthorizedDistributedData {
        static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS, TestStaticKeyData.class.getSimpleName());

        private final String content;
        private final Set<String> staticKeys;
        private final boolean staticPublicKeyRequired;
        private final boolean staticPublicKeysProvided;

        private TestStaticKeyData(String content,
                                  Set<String> staticKeys,
                                  boolean staticPublicKeyRequired,
                                  boolean staticPublicKeysProvided) {
            this.content = content;
            this.staticKeys = staticKeys;
            this.staticPublicKeyRequired = staticPublicKeyRequired;
            this.staticPublicKeysProvided = staticPublicKeysProvided;
        }

        static TestStaticKeyData requiringStaticKey(String content,
                                                    Set<String> staticKeys,
                                                    boolean staticPublicKeysProvided) {
            return new TestStaticKeyData(content, staticKeys, true, staticPublicKeysProvided);
        }

        static TestStaticKeyData withoutRequirement(String content,
                                                    Set<String> staticKeys,
                                                    boolean staticPublicKeysProvided) {
            return new TestStaticKeyData(content, staticKeys, false, staticPublicKeysProvided);
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(content);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public Set<String> getAuthorizedPublicKeys() {
            return staticKeys;
        }

        @Override
        public boolean staticPublicKeysProvided() {
            return staticPublicKeysProvided;
        }

        @Override
        public boolean isStaticPublicKeyRequired() {
            return staticPublicKeyRequired;
        }
    }
}
