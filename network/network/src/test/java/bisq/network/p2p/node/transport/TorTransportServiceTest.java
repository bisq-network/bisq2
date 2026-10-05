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

package bisq.network.p2p.node.transport;

import bisq.common.network.TorAddress;
import bisq.network.tor.TorService;
import bisq.network.tor.TorTransportConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.net.Socket;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TorTransportServiceTest {
    private static final String ONION = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwx.onion";

    // The Tor service is static in TorTransportService, so each test starts and ends without one
    @BeforeEach
    @AfterEach
    void resetTorService() throws Exception {
        setStaticTorService(null);
    }

    @Test
    void connectionOfANodeUsesTheNodeIdAsStreamId() throws Exception {
        TorTransportConfig config = mock(TorTransportConfig.class);
        when(config.getSocketTimeout()).thenReturn(120_000);
        try (MockedConstruction<TorService> torServices = mockConstruction(TorService.class,
                (torService, context) -> when(torService.getSocket(anyString(), anyInt(), anyString(), anyInt()))
                        .thenReturn(mock(Socket.class)))) {
            TorTransportService transportService = new TorTransportService(config);

            transportService.getSocket(new TorAddress(ONION, 1000), "node1");
            transportService.getSocket(new TorAddress(ONION, 1000), "node2");

            TorService torService = torServices.constructed().get(0);
            verify(torService).getSocket(eq(ONION), eq(1000), eq("node1"), eq(120_000));
            verify(torService).getSocket(eq(ONION), eq(1000), eq("node2"), eq(120_000));
        }
    }

    private static void setStaticTorService(TorService torService) throws Exception {
        Field field = TorTransportService.class.getDeclaredField("torService");
        field.setAccessible(true);
        field.set(null, torService);
    }
}
