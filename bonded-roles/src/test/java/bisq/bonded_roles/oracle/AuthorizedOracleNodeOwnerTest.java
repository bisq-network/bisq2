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

package bisq.bonded_roles.oracle;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.ExceptionUtil;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthorizedOracleNode names the key of its network ID as its owner. Nodes of v2.1.13 and earlier accept it only from
 * that key. This version also accepts the authorized key as publisher, but only if it is a static oracle node key
 * (see rule 4 of docs/specifications/network/storage-ownership.md).
 * <p>
 * The network storage checks only the signature of authorized data without the static flag. So without the limit to
 * static keys, any node could publish an AuthorizedOracleNode with its own key as authorized key and as publisher.
 * Nodes of this version would store and relay it. Nodes of v2.1.13 reject it each time they receive it, so nodes of
 * this version would send it to them again with every inventory response.
 * <p>
 * The tests use the real store of the DataService.
 */
class AuthorizedOracleNodeOwnerTest {
    // The first of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS. In dev mode they are the static keys.
    private static final String DEV_PRIVATE_KEY = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";

    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    // The key pair of the network ID of the oracle node
    private KeyPair identityKeyPair;
    // An authorized key pair which is no static key, as any node can create it
    private KeyPair authorizedKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedOracleNode.class, AuthorizedOracleNode.META_DATA);
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedOracleNode", AuthorizedOracleNode.class,
                AuthorizedOracleNode.getResolver());
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedOracleNode.class.getSimpleName())
                .join();
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    // As oracle nodes publish it, and as nodes of v2.1.13 and earlier require it
    @Test
    void oracleNodePublishedByTheKeyOfItsNetworkIdIsAccepted() throws GeneralSecurityException {
        AuthorizedData publication = authorize(createOracleNode(authorizedKeyPair.getPublic(), false), authorizedKeyPair);

        DataStorageResult result = store.add(AddAuthenticatedDataRequest.from(store, publication, identityKeyPair));

        assertThat(result.isSuccess()).as(result.toString()).isTrue();
        assertThat(store.getPersistableStore().getMap()).hasSize(1);
    }

    @Test
    void oracleNodeCannotBePublishedByAnAuthorizedKeyWhichIsNoStaticKey() throws GeneralSecurityException {
        AuthorizedData publication = authorize(createOracleNode(authorizedKeyPair.getPublic(), false), authorizedKeyPair);

        assertThat(publication.isOwner(DigestUtil.hash(authorizedKeyPair.getPublic().getEncoded()))).isFalse();
        assertThatThrownBy(() -> AddAuthenticatedDataRequest.from(store, publication, authorizedKeyPair))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not the owner");
        assertThat(store.getPersistableStore().getMap()).isEmpty();
    }

    // A modified node can send such a request. It fails at parsing, so a node of this version neither stores nor
    // relays it (rule 5 of docs/specifications/network/storage-ownership.md).
    @Test
    void receivedOracleNodeOfAnAuthorizedKeyWhichIsNoStaticKeyFailsAtParsing() throws Exception {
        AuthorizedData publication = authorize(createOracleNode(authorizedKeyPair.getPublic(), false), authorizedKeyPair);
        bisq.network.protobuf.AddAuthenticatedDataRequest requestOfIdentityKey = AddAuthenticatedDataRequest.from(store,
                publication, identityKeyPair).toValueProto(false);
        // The publisher hash still names the key of the request, so only the owner rule is broken
        byte[] authorizedPublicKeyBytes = authorizedKeyPair.getPublic().getEncoded();
        bisq.network.protobuf.AddAuthenticatedDataRequest requestOfAuthorizedKey = requestOfIdentityKey.toBuilder()
                .setOwnerPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes))
                .setAuthenticatedSequentialData(requestOfIdentityKey.getAuthenticatedSequentialData().toBuilder()
                        .setPubKeyHash(ByteString.copyFrom(DigestUtil.hash(authorizedPublicKeyBytes))))
                .build();

        assertThat(AddAuthenticatedDataRequest.fromProto(requestOfIdentityKey).getDistributedData())
                .isEqualTo(publication.getDistributedData());
        assertThatThrownBy(() -> AddAuthenticatedDataRequest.fromProto(requestOfAuthorizedKey))
                .isInstanceOf(UnresolvableProtobufException.class)
                .satisfies(exception -> assertThat(ExceptionUtil.getRootCauseMessage(exception))
                        .contains("not the owner"));
    }

    // In a later version the root oracle nodes publish their AuthorizedOracleNode also with their authorized key pair.
    // The owner rule does not depend on the static flag, which the publisher sets.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void oracleNodePublishedByAStaticAuthorizedKeyIsAccepted(boolean staticPublicKeysProvided) throws GeneralSecurityException {
        DevMode.setDevMode(true);
        try {
            KeyPair staticKeyPair = new KeyPair(KeyGeneration.generatePublic(Hex.decode(DEV_PUBLIC_KEY)),
                    KeyGeneration.generatePrivate(Hex.decode(DEV_PRIVATE_KEY)));
            AuthorizedData publication = authorize(createOracleNode(staticKeyPair.getPublic(), staticPublicKeysProvided),
                    staticKeyPair);

            DataStorageResult resultOfIdentityKey = store.add(AddAuthenticatedDataRequest.from(store, publication,
                    identityKeyPair));
            DataStorageResult resultOfAuthorizedKey = store.add(AddAuthenticatedDataRequest.from(store, publication,
                    staticKeyPair));

            assertThat(resultOfIdentityKey.isSuccess()).as(resultOfIdentityKey.toString()).isTrue();
            assertThat(resultOfAuthorizedKey.isSuccess()).as(resultOfAuthorizedKey.toString()).isTrue();
            assertThat(store.getPersistableStore().getMap()).hasSize(2);
            // Another key is still no owner
            assertThatThrownBy(() -> AddAuthenticatedDataRequest.from(store, publication, authorizedKeyPair))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            DevMode.setDevMode(false);
        }
    }

    // Outside of dev mode the static keys are the oracle node keys of this version. The test has no private key of
    // them, so it checks the owner rule without an add request.
    @Test
    void oracleNodeKeyOfThisVersionOwnsItsOracleNode() {
        PublicKey oracleNodeKey = KeyGeneration.getPublicKeyFromHex(AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS.iterator().next());
        AuthorizedData publication = new AuthorizedData(createOracleNode(oracleNodeKey, true), oracleNodeKey);

        assertThat(publication.isOwner(DigestUtil.hash(oracleNodeKey.getEncoded()))).isTrue();
        assertThat(publication.isOwner(DigestUtil.hash(identityKeyPair.getPublic().getEncoded()))).isTrue();
        assertThat(publication.isOwner(DigestUtil.hash(authorizedKeyPair.getPublic().getEncoded()))).isFalse();
    }

    private AuthorizedOracleNode createOracleNode(PublicKey authorizedPublicKey, boolean staticPublicKeysProvided) {
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                LocalHostAddressTypeFacade.toLocalHostAddress(1234))),
                new PubKey(identityKeyPair.getPublic(), "identity"));
        return new AuthorizedOracleNode(networkId,
                "a".repeat(40),
                Hex.encode(authorizedPublicKey.getEncoded()),
                "oracle",
                "signature",
                staticPublicKeysProvided);
    }

    private static AuthorizedData authorize(AuthorizedOracleNode data, KeyPair keyPair) throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}
