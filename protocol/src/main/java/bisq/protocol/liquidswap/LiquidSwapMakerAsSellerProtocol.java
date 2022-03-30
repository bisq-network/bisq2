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

package bisq.protocol.liquidswap;

import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceClient;
import bisq.protocol.MakerProtocolModel;
import bisq.protocol.ProtocolStore;
import bisq.protocol.SellerProtocol;
import com.google.protobuf.Message;

public class LiquidSwapMakerAsSellerProtocol extends LiquidSwapMakerProtocol implements SellerProtocol {

    public LiquidSwapMakerAsSellerProtocol(NetworkService networkService,
                                           PersistenceClient<ProtocolStore> persistenceClient,
                                           MakerProtocolModel protocolModel,
                                           NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService,
                persistenceClient,
                protocolModel,
                myNodeIdAndKeyPair);
    }

    @Override
    public Message toProto() {
        //todo
        return null;
    }
}