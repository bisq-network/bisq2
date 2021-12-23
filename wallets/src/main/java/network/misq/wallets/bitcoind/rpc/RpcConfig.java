package network.misq.wallets.bitcoind.rpc;

import network.misq.wallets.NetworkType;

import java.util.Optional;

public record RpcConfig(NetworkType networkType,
                        String hostname,
                        int port,
                        String user,
                        String password,
                        Optional<String> walletName) {

    public static class Builder {
        private static final int INVALID_PORT = -1;

        private NetworkType networkType;

        private String hostname;
        private String user;
        private String password;

        private Integer port = INVALID_PORT;
        private Optional<String> walletName = Optional.empty();

        public Builder() {
        }

        public Builder(RpcConfig configTemplate) {
            this.networkType = configTemplate.networkType();
            this.hostname = configTemplate.hostname();
            this.port = configTemplate.port();

            this.user = configTemplate.user();
            this.password = configTemplate.password();
            this.walletName = configTemplate.walletName();
        }

        public RpcConfig build() {
            if (port == INVALID_PORT) {
                port = networkType.getRpcPort();
            }
            return new RpcConfig(networkType, hostname, port, user, password, walletName);
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

        public RpcConfig.Builder walletName(String walletName) {
            this.walletName = Optional.of(walletName);
            return this;
        }
    }
}
