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

package bisq.identity;

import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IdentityServiceTest {
    @TempDir
    private Path tempDir;
    private KeyPairService keyPairService;
    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        List<Node> initializedNodes = Collections.emptyList();
        doReturn(CompletableFuture.completedFuture(initializedNodes))
                .when(networkService).getAllInitializedNodes(any(), any());

        keyPairService = new KeyPairService(persistenceService);
        identityService = new IdentityService(persistenceService, keyPairService, networkService);
    }

    @AfterEach
    void tearDown() {
        keyPairService.getPersistence().flush().join();
        identityService.getPersistence().flush().join();
    }

    @Test
    void getOrCreateIdentityTest() {
        String myTag = "myTag";
        Identity activeIdentity = identityService.getOrCreateIdentity(myTag);
        Identity persistedActiveIdentity = identityService.getOrCreateIdentity(myTag);

        assertThat(activeIdentity.getTag())
                .isEqualTo(myTag);
        assertThat(activeIdentity).isSameAs(persistedActiveIdentity);
    }

    @Test
    void getOrCreateIdentityWithAllArguments() {
        String identityTag = "myTag1";
        String keyId = keyPairService.getKeyIdFromTag(identityTag);
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        Identity activeIdentity = identityService.findActiveIdentity(identityTag)
                .orElseGet(() -> identityService.createAndInitializeNewActiveIdentity(identityTag, keyId, keyPair).join());

        assertThat(activeIdentity.getTag()).isEqualTo(identityTag);
        assertThat(activeIdentity.getNetworkId().getPubKey().getKeyId()).isEqualTo(keyId);
        assertThat(activeIdentity.getKeyPair()).isEqualTo(keyPair);

        Identity persistedActiveIdentity = identityService.findActiveIdentity(identityTag)
                .orElseGet(() -> identityService.createAndInitializeNewActiveIdentity(identityTag, keyId, keyPair).join());
        assertThat(activeIdentity).isSameAs(persistedActiveIdentity);
    }

    @Test
    void getOrCreateDefaultIdentityTest() {
        Identity firstDefaultIdentity = identityService.getOrCreateDefaultIdentity();
        assertThat(firstDefaultIdentity.getTag())
                .isEqualTo(IdentityService.DEFAULT_IDENTITY_TAG);

        Identity secondDefaultIdentity = identityService.getOrCreateDefaultIdentity();
        assertThat(firstDefaultIdentity).isSameAs(secondDefaultIdentity);
    }

    @Test
    void createNewIdentity() {
        String myTag = "myTag";
        Identity activeIdentity = identityService.createAndInitializeNewActiveIdentity(myTag).join();
        Identity anotherActiveIdentity = identityService.createAndInitializeNewActiveIdentity(myTag).join();

        assertThat(activeIdentity).isNotSameAs(anotherActiveIdentity);
    }

    @Test
    void retireInvalidIdentity() {
        boolean isRemoved = identityService.retireActiveIdentity("tag");
        assertThat(isRemoved).isFalse();
    }

    @Test
    void retireActiveIdentity() {
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        boolean isRemoved = identityService.retireActiveIdentity(myTag);
        assertThat(isRemoved).isTrue();

        Optional<Identity> activeIdentity = identityService.findActiveIdentity(myTag);
        assertThat(activeIdentity).isEmpty();

        Identity defaultIdentity = identityService.getOrCreateDefaultIdentity();
        assertThat(identity).isNotEqualTo(defaultIdentity);

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(identity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(identity);
    }

    @Test
    void findInvalidIdentity() {
        Optional<Identity> activeIdentity = identityService.findActiveIdentity("tag");
        assertThat(activeIdentity).isEmpty();
    }

    @Test
    void findInvalidIdentityByNetworkId() {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, Address.localHost(1234)));

        String keyId = keyPairService.getKeyIdFromTag("myTag2");
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        var pubKey = new PubKey(keyPair.getPublic(), keyId);
        var networkId = new NetworkId(addressByTransportTypeMap, pubKey);

        Optional<Identity> activeIdentity = identityService.findActiveIdentity(networkId);
        assertThat(activeIdentity).isEmpty();
    }

    @Test
    void findActiveIdentity() {
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> activeIdentity = identityService.findActiveIdentity(myTag);
        assertThat(activeIdentity).hasValue(identity);
    }

    @Test
    void findActiveIdentityByNetworkId() {
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> activeIdentity = identityService.findActiveIdentity(identity.getNetworkId());
        assertThat(activeIdentity).hasValue(identity);
    }

    @Test
    void findInvalidRetiredIdentity() {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, Address.localHost(1234)));
        String keyId = keyPairService.getKeyIdFromTag("myTag3");
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        var pubKey = new PubKey(keyPair.getPublic(), keyId);
        var networkId = new NetworkId(addressByTransportTypeMap, pubKey);

        Optional<Identity> retiredIdentity = identityService.findRetiredIdentityByNetworkId(networkId);
        assertThat(retiredIdentity).isEmpty();
    }

    @Test
    void findRetiredIdentity() {
        Identity identity = identityService.getOrCreateIdentity("tag");
        identityService.retireActiveIdentity("tag");

        Optional<Identity> retiredIdentity = identityService.findRetiredIdentityByNetworkId(identity.getNetworkId());
        assertThat(retiredIdentity).hasValue(identity);
    }

    @Test
    void findDefaultIdentityInFindAnyIdentity() {
        Identity defaultIdentity = identityService.getOrCreateDefaultIdentity();
        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(defaultIdentity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(defaultIdentity);
    }

    @Test
    void findActiveIdentityInFindAnyIdentity() {
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(identity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(identity);
    }

    @Test
    void findRetiredIdentityInFindAnyIdentity() {
        Identity identity = identityService.getOrCreateIdentity("tag");
        identityService.retireActiveIdentity("tag");

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(identity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(identity);
    }
}
