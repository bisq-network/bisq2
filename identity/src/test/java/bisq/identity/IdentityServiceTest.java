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

import bisq.common.network.DefaultClearNetLocalAddressFacade;
import bisq.network.NetworkService;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// TODO mocking the initialization, methods and fields of networkService makes that test too complex

@Disabled
public class IdentityServiceTest {
    @TempDir
    private Path tempDir;
    private KeyBundleService keyBundleService;
    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        Node node = mock(Node.class);
        Map<TransportType, CompletableFuture<Node>> map = new HashMap<>();
        map.put(TransportType.TOR, CompletableFuture.completedFuture(node));
        when(networkService.getInitializedDefaultNodeByTransport()).thenReturn(map);

        doReturn(CompletableFuture.completedFuture(mock(Node.class)))
                .when(networkService).anySuppliedInitializedNode(any());

        keyBundleService = new KeyBundleService(persistenceService, mock(KeyBundleService.Config.class));
        identityService = new IdentityService(persistenceService, keyBundleService, networkService);
        identityService.initialize().join();

        // TODO would require more mocks to work
        networkService.initialize().join();
    }

    @AfterEach
    void tearDown() {
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
        // String keyId = keyBundleService.getKeyIdFromTag(identityTag);
        // KeyPair keyPair = keyBundleService.getOrCreateKeyBundle(keyId).getKeyPair();
        Identity activeIdentity = identityService.findActiveIdentity(identityTag)
                .orElseGet(() -> identityService.createAndInitializeNewActiveIdentity(identityTag).join());

        assertThat(activeIdentity.getTag()).isEqualTo(identityTag);
        // assertThat(activeIdentity.getNetworkId().getPubKey().getKeyId()).isEqualTo(keyId);
        // assertThat(activeIdentity.getKeyBundle().getKeyPair()).isEqualTo(keyPair);

        Identity persistedActiveIdentity = identityService.findActiveIdentity(identityTag)
                .orElseGet(() -> identityService.createAndInitializeNewActiveIdentity(identityTag).join());
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
                Map.of(TransportType.CLEAR, DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234)));

        String keyId = keyBundleService.getKeyIdFromTag("myTag2");
        KeyPair keyPair = keyBundleService.getOrCreateKeyBundle(keyId).getKeyPair();
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
                Map.of(TransportType.CLEAR, DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234)));
        String keyId = keyBundleService.getKeyIdFromTag("myTag3");
        KeyPair keyPair = keyBundleService.getOrCreateKeyBundle(keyId).getKeyPair();
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
