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

import bisq.common.util.NetworkUtils;
import bisq.wallets.core.RpcConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Random;

@Builder
@Getter
public class ElectrumConfig {

    public static class Generator {
        private static final int LOWER_CASE_A_ASCII_INDEX = 97;
        private static final int LOWER_CASE_Z_ASCII_INDEX = 122;
        private static final int RANDOM_STRING_LENGTH = 16;

        public static ElectrumConfig generate() {
            return ElectrumConfig.builder()
                    .rpcUser(generateRandomAlphaString())
                    .rpcPassword(generateRandomAlphaString())
                    .rpcPort(String.valueOf(NetworkUtils.findFreeSystemPort()))
                    .build();
        }
        private static String generateRandomAlphaString() {
            return new Random().ints(LOWER_CASE_A_ASCII_INDEX, LOWER_CASE_Z_ASCII_INDEX + 1)
                    .limit(RANDOM_STRING_LENGTH)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }
    }

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
