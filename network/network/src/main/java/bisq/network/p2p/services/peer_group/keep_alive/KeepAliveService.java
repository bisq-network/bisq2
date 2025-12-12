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

package bisq.network.p2p.services.peer_group.keep_alive;

import bisq.common.timer.Scheduler;
import bisq.network.p2p.common.RequestResponseHandler;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class KeepAliveService extends RequestResponseHandler<Ping, Pong> {
    @Getter
    @ToString
    public static final class Config {
        private final long maxIdleTime;
        private final long interval;
        private final long timeout;

        public Config(long maxIdleTime, long interval, long timeout) {
            this.maxIdleTime = maxIdleTime;
            this.interval = interval;
            this.timeout = timeout;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new Config(
                    SECONDS.toMillis(typesafeConfig.getLong("maxIdleTimeInSeconds")),
                    SECONDS.toMillis(typesafeConfig.getLong("intervalInSeconds")),
                    SECONDS.toMillis(typesafeConfig.getLong("timeoutInSeconds"))
            );
        }
    }

    private final Config config;
    private Optional<Scheduler> scheduler = Optional.empty();

    public KeepAliveService(Node node, Config config) {
        super(node, config.getTimeout());
        this.config = config;
    }

    public void initialize() {
        super.initialize();
        scheduler = Optional.of(Scheduler.run(this::sendPingIfRequired)
                .host(this)
                .runnableName("sendPingIfRequired")
                .periodically(config.getInterval()));
    }

    public void shutdown() {
        super.shutdown();
        scheduler.ifPresent(Scheduler::stop);
        scheduler = Optional.empty();
    }

    @Override
    protected Pong createResponse(Connection connection, Ping request) {
        return new Pong(request.getNonce());
    }

    @Override
    protected Class<Ping> getRequestClass() {
        return Ping.class;
    }

    @Override
    protected Class<Pong> getResponseClass() {
        return Pong.class;
    }

    private void sendPingIfRequired() {
        node.getAllActiveConnections()
                .filter(this::isRequired)
                .forEach(this::request);
    }

    private void request(Connection connection) {
        super.request(connection, new Ping(createNonce()));
    }

    private boolean isRequired(Connection connection) {
        return System.currentTimeMillis() - connection.getConnectionMetrics().getLastUpdate().get() > config.getMaxIdleTime();
    }
}