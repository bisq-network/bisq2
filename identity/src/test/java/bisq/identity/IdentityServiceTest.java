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
import bisq.network.common.TransportType;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityServiceTest {
    @Test
    void defaultIdentityPersistence(@TempDir Path tempDir) {
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
}
