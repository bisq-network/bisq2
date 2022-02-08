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

package bisq.wallets.bitcoind.rpc;

import bisq.wallets.NetworkType;

public record RpcConfig(NetworkType networkType,
                        String hostname,
                        int port,
                        String user,
                        String password) {

    public static class Builder {
        private static final int INVALID_PORT = -1;

        private NetworkType networkType;

        private String hostname;
        private String user;
        private String password;

        private Integer port = INVALID_PORT;

        public Builder() {
        }

        public Builder(RpcConfig configTemplate) {
            this.networkType = configTemplate.networkType();
            this.hostname = configTemplate.hostname();
            this.port = configTemplate.port();

            this.user = configTemplate.user();
            this.password = configTemplate.password();
        }

        public RpcConfig build() {
            if (port == INVALID_PORT) {
                port = networkType.getRpcPort();
            }
            return new RpcConfig(networkType, hostname, port, user, password);
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
    }
}
