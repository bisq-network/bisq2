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

package bisq.wallets.rpc;

import bisq.wallets.NetworkType;

import java.nio.file.Path;
import java.util.Objects;

public record RpcConfig(NetworkType networkType,
                        String hostname,
                        int port,
                        String user,
                        String password,
                        Path walletPath) {

    public static class Builder {
        private static final int INVALID_PORT = -1;

        private NetworkType networkType;

        private String hostname;
        private Integer port = INVALID_PORT;

        private String user;
        private String password;

        private Path walletPath;

        public Builder() {
        }

        public Builder(RpcConfig configTemplate) {
            this.networkType = configTemplate.networkType();
            this.hostname = configTemplate.hostname();
            this.port = configTemplate.port();

            this.user = configTemplate.user();
            this.password = configTemplate.password();
            this.walletPath = configTemplate.walletPath();
        }

        public RpcConfig build() {
            Objects.requireNonNull(networkType);
            Objects.requireNonNull(hostname);

            if (port == INVALID_PORT) {
                throw new IllegalStateException("Port must be set.");
            }

            Objects.requireNonNull(user);
            Objects.requireNonNull(password);
            Objects.requireNonNull(walletPath);

            return new RpcConfig(networkType, hostname, port, user, password, walletPath);
        }

        public RpcConfig.Builder networkType(NetworkType networkType) {
            this.networkType = networkType;
            return this;
        }

        public RpcConfig.Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public RpcConfig.Builder user(String user) {
            this.user = user;
            return this;
        }

        public RpcConfig.Builder password(String password) {
            this.password = password;
            return this;
        }

        public RpcConfig.Builder port(int port) {
            this.port = port;
            return this;
        }

        public RpcConfig.Builder walletPath(Path walletPath) {
            this.walletPath = walletPath;
            return this;
        }
    }
}
