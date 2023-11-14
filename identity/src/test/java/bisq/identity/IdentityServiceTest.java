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
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityServiceTest {
    @Test
    void getOrCreateIdentityTest(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);

        String myTag = "myTag";
        Identity activeIdentity = identityService.getOrCreateIdentity(myTag);
        Identity persistedActiveIdentity = identityService.getOrCreateIdentity(myTag);

        assertThat(activeIdentity.getTag())
                .isEqualTo(myTag);
        assertThat(activeIdentity).isSameAs(persistedActiveIdentity);
    }

    @Test
    void getOrCreateIdentityWithAllArguments(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);

        String myTag = "myTag";
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(myTag);
        Identity activeIdentity = identityService.getOrCreateIdentity(myTag, myTag, keyPair);

        assertThat(activeIdentity.getTag())
                .isEqualTo(myTag);
        assertThat(activeIdentity.getNetworkId().getPubKey().getKeyId())
                .isEqualTo(myTag);
        assertThat(activeIdentity.getKeyPair())
                .isEqualTo(keyPair);

        Identity persistedActiveIdentity = identityService.getOrCreateIdentity(myTag, myTag, keyPair);
        assertThat(activeIdentity).isSameAs(persistedActiveIdentity);
    }

    @Test
    void getOrCreateDefaultIdentityTest(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);

        Identity firstDefaultIdentity = identityService.getOrCreateDefaultIdentity();
        assertThat(firstDefaultIdentity.getTag())
                .isEqualTo(IdentityService.DEFAULT_IDENTITY_TAG);

        Identity secondDefaultIdentity = identityService.getOrCreateDefaultIdentity();
        assertThat(firstDefaultIdentity).isSameAs(secondDefaultIdentity);
    }

    @Test
    void createNewIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);

        String myTag = "myTag";
        Identity activeIdentity = identityService.createAndInitializeNewActiveIdentity(myTag);
        Identity anotherActiveIdentity = identityService.createAndInitializeNewActiveIdentity(myTag);

        assertThat(activeIdentity).isNotSameAs(anotherActiveIdentity);
    }

    @Test
    void retireInvalidIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);
        NetworkService networkService = mock(NetworkService.class);

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        boolean isRemoved = identityService.retireActiveIdentity("tag");
        assertThat(isRemoved).isFalse();
    }

    @Test
    void retireActiveIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
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
    void findInvalidIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        Optional<Identity> activeIdentity = identityService.findActiveIdentity("tag");
        assertThat(activeIdentity).isEmpty();
    }

    @Test
    void findInvalidIdentityByNetworkId(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, Address.localHost(1234)));

        KeyPair keyPair = keyPairService.getOrCreateKeyPair("keyId");
        var pubKey = new PubKey(keyPair.getPublic(), "keyId");
        var networkId = new NetworkId(addressByTransportTypeMap, pubKey);

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        Optional<Identity> activeIdentity = identityService.findActiveIdentityByNetworkId(networkId);
        assertThat(activeIdentity).isEmpty();
    }

    @Test
    void findActiveIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> activeIdentity = identityService.findActiveIdentity(myTag);
        assertThat(activeIdentity).hasValue(identity);
    }

    @Test
    void findActiveIdentityByNetworkId(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> activeIdentity = identityService.findActiveIdentityByNetworkId(identity.getNetworkId());
        assertThat(activeIdentity).hasValue(identity);
    }

    @Test
    void findInvalidRetiredIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);

        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, Address.localHost(1234)));

        KeyPair keyPair = keyPairService.getOrCreateKeyPair("keyId");
        var pubKey = new PubKey(keyPair.getPublic(), "keyId");
        var networkId = new NetworkId(addressByTransportTypeMap, pubKey);

        Optional<Identity> retiredIdentity = identityService.findRetiredIdentityByNetworkId(networkId);
        assertThat(retiredIdentity).isEmpty();
    }

    @Test
    void findRetiredIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        Identity identity = identityService.getOrCreateIdentity("tag");
        identityService.retireActiveIdentity("tag");

        Optional<Identity> retiredIdentity = identityService.findRetiredIdentityByNetworkId(identity.getNetworkId());
        assertThat(retiredIdentity).hasValue(identity);
    }

    @Test
    void findDefaultIdentityInFindAnyIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        Identity defaultIdentity = identityService.getOrCreateDefaultIdentity();

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(defaultIdentity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(defaultIdentity);
    }

    @Test
    void findActiveIdentityInFindAnyIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        String myTag = "myTag";
        Identity identity = identityService.getOrCreateIdentity(myTag);

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(identity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(identity);
    }

    @Test
    void findRetiredIdentityInFindAnyIdentity(@TempDir Path tempDir) {
        var persistenceService = new PersistenceService(tempDir.toAbsolutePath().toString());
        var keyPairService = new KeyPairService(persistenceService);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));

        var identityService = new IdentityService(persistenceService, keyPairService, networkService);
        Identity identity = identityService.getOrCreateIdentity("tag");
        identityService.retireActiveIdentity("tag");

        Optional<Identity> identityByNetworkId = identityService
                .findAnyIdentityByNetworkId(identity.getNetworkId());
        assertThat(identityByNetworkId).hasValue(identity);
    }
}
