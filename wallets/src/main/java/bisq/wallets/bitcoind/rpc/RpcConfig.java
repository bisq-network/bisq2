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
