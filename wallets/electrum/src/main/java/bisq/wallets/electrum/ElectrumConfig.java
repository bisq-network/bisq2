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

package bisq.wallets.electrum;

import bisq.wallets.core.RpcConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ElectrumConfig {
    @JsonProperty("log_to_file")
    private final String logToFile = "true";
    @JsonProperty("rpchost")
    private final String rpcHost = "127.0.0.1";

    @JsonProperty("rpcpassword")
    private final String rpcPassword;
    @JsonProperty("rpcport")
    private final String rpcPort;
    @JsonProperty("rpcuser")
    private final String rpcUser;

    public ElectrumConfig(String rpcPassword, String rpcPort, String rpcUser) {
        this.rpcPassword = rpcPassword;
        this.rpcPort = rpcPort;
        this.rpcUser = rpcUser;
    }

    public RpcConfig toRpcConfig() {
        return RpcConfig.builder()
                .hostname(rpcHost)
                .port(Integer.parseInt(rpcPort))
                .user(rpcUser)
                .password(rpcPassword)
                .build();
    }
}
