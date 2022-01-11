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

package bisq.desktop.primary.main.content.social.hangout;

import bisq.application.DefaultServiceProvider;
import bisq.common.data.Pair;
import bisq.desktop.common.view.Model;
import bisq.desktop.primary.main.content.social.tradeintent.TradeIntent;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.NetworkId;
import bisq.security.KeyPairService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class HangoutModel implements Model {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final KeyPairService keyPairService;
    private Optional<TradeIntent> tradeIntent = Optional.empty();
    private Optional<NetworkId> networkId = Optional.empty();
    public StringProperty chatText = new SimpleStringProperty("");
    public ObservableList<String> chatPeers = FXCollections.observableArrayList();
    public Optional<String> selectedChatPeer = Optional.empty();

    public HangoutModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        keyPairService = serviceProvider.getKeyPairService();
    }

    void setData(Object data) {
        Pair<TradeIntent, NetworkId> pair = Pair.class.cast(data);
        tradeIntent = Optional.of(pair.first());
        networkId = Optional.of(pair.second());

        String userId = tradeIntent.get().userId();
        setSelectedChatPeer(userId);
        if (!chatPeers.contains(userId)) {
            chatPeers.add(userId);
        }
    }

    public void setSelectedChatPeer(String chatPeer) {
        selectedChatPeer = Optional.of(chatPeer);
    }

    public void send(String text) {
        chatText.set(chatText.get() + ">> " + text + "\n");
    }
}
