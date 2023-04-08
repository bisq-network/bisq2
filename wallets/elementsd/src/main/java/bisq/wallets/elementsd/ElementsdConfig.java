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

package bisq.wallets.elementsd;


import bisq.wallets.json_rpc.RpcConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ElementsdConfig {
    private final RpcConfig bitcoindRpcConfig;
    private final RpcConfig elementsdRpcConfig;

    public ElementsdConfig(RpcConfig bitcoindRpcConfig, RpcConfig elementsdRpcConfig) {
        this.bitcoindRpcConfig = bitcoindRpcConfig;
        this.elementsdRpcConfig = elementsdRpcConfig;
    }

    public RpcConfig bitcoindRpcConfig() {
        return bitcoindRpcConfig;
    }

    public RpcConfig elementsdRpcConfig() {
        return elementsdRpcConfig;
    }
}
