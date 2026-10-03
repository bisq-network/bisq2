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

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An identity is identified by its key. The addresses of its network ID are routing data: they change when the user
 * adds a transport, while offers and trades keep the network ID with the earlier addresses.
 */
class IdentityServiceFindByIdTest {
    private final Identity defaultIdentity = identity(IdentityService.DEFAULT_IDENTITY_TAG, 1000);
    private final Identity activeIdentity = identity("active", 1001);
    private final Identity retiredIdentity = identity("retired", 1002);

    private IdentityService identityService;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(mock(Persistence.class));
        identityService = new IdentityService(persistenceService,
                mock(KeyBundleService.class),
                mock(NetworkService.class));
        identityService.getPersistableStore().applyPersisted(new IdentityStore(Optional.of(defaultIdentity),
                Map.of(activeIdentity.getTag(), activeIdentity),
                Set.of(retiredIdentity)));
    }

    @Test
    void identityIsFoundByItsKeyWhenTheAddressesOfTheNetworkIdDiffer() {
        NetworkId withEarlierAddresses = withOtherAddress(activeIdentity.getNetworkId(), 2001);

        assertThat(identityService.findAnyIdentityById(withEarlierAddresses.getId())).hasValue(activeIdentity);
        // The lookup by the whole network ID compares the addresses as well
        assertThat(identityService.findAnyIdentityByNetworkId(withEarlierAddresses)).isEmpty();
    }

    @Test
    void defaultActiveAndRetiredIdentitiesAreFound() {
        assertThat(identityService.findAnyIdentityById(defaultIdentity.getId())).hasValue(defaultIdentity);
        assertThat(identityService.findAnyIdentityById(activeIdentity.getId())).hasValue(activeIdentity);
        assertThat(identityService.findAnyIdentityById(retiredIdentity.getId())).hasValue(retiredIdentity);
    }

    @Test
    void identityOfAnotherKeyIsNotFound() {
        Identity otherIdentity = identity("other", 1001);

        assertThat(identityService.findAnyIdentityById(otherIdentity.getId())).isEmpty();
    }

    private static Identity identity(String tag, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), tag));
        return new Identity(tag, networkId, new KeyBundle(tag, keyPair, TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair()));
    }

    private static NetworkId withOtherAddress(NetworkId networkId, int port) {
        Address otherAddress = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
