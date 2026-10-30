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

package bisq.network;

import bisq.common.network.TransportType;
import bisq.network.p2p.node.Node;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The network is ready with its first initialized transport, so a failed transport next to it, for example a Tor
 * whose start the Tor library refused, has to reach the log on its own.
 */
class NetworkServiceFailedTransportLogTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(NetworkService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void aTransportWhichFailedBeforeTheNetworkWasReadyIsLoggedWithItsCause() {
        CompletableFuture<Node> tor = CompletableFuture.failedFuture(
                new CompletionException(new IllegalStateException("Bisq does not run on a Whonix-Gateway")));
        CompletableFuture<Node> i2p = CompletableFuture.completedFuture(mock(Node.class));

        NetworkService.logFailedTransports(Map.of(TransportType.TOR, tor, TransportType.I2P, i2p));

        List<ILoggingEvent> errors = errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getFormattedMessage()).contains("TOR");
        assertThat(errors.get(0).getThrowableProxy().getCause().getMessage())
                .isEqualTo("Bisq does not run on a Whonix-Gateway");
    }

    @Test
    void aTransportWhichFailsAfterTheNetworkWasReadyIsLogged() {
        CompletableFuture<Node> tor = new CompletableFuture<>();
        CompletableFuture<Node> i2p = CompletableFuture.completedFuture(mock(Node.class));
        NetworkService.logFailedTransports(Map.of(TransportType.TOR, tor, TransportType.I2P, i2p));
        assertThat(errors()).isEmpty();

        tor.completeExceptionally(new IllegalStateException("The SOCKS port does not isolate streams"));

        assertThat(errors()).hasSize(1);
        assertThat(errors().get(0).getFormattedMessage()).contains("TOR");
    }

    @Test
    void initializedTransportsAreNotLogged() {
        NetworkService.logFailedTransports(Map.of(
                TransportType.TOR, CompletableFuture.completedFuture(mock(Node.class)),
                TransportType.I2P, CompletableFuture.completedFuture(mock(Node.class))));

        assertThat(errors()).isEmpty();
    }

    private List<ILoggingEvent> errors() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
    }
}
