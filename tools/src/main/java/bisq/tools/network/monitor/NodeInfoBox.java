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

package bisq.tools.network.monitor;

import bisq.common.encoding.Hex;
import bisq.desktop.common.threading.UIScheduler;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.security.DigestUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NodeInfoBox extends VBox {
    private final Button button;
    private final Label info;
    private final Transport.Type transportType;
    private final Address address;
    private final boolean isSeed;
    private final List<String> list = new ArrayList<>();

    public NodeInfoBox(Address address, Transport.Type transportType, boolean isSeed) {
        this.address = address;
        this.transportType = transportType;
        this.isSeed = isSeed;
        button = new Button(getTitle());
        info = new Label();
        setSpacing(5);
        getChildren().addAll(button, info);
    }

    private String getTitle() {
        String name = isSeed ? "-Seed: " : "-Node: ";
        return transportType.name() + name + address;
    }

    public void onData(NetworkPayload networkPayload) {
        byte[] hash = DigestUtil.hash(networkPayload.serialize());
        String id = Hex.encode(hash).substring(0, 8);
        int red = 128 + new BigInteger(hash).mod(BigInteger.valueOf(128)).intValue();
        hash = DigestUtil.hash(hash);
        int green = 128 + new BigInteger(hash).mod(BigInteger.valueOf(128)).intValue();
        hash = DigestUtil.hash(hash);
        int blue = 128 + new BigInteger(hash).mod(BigInteger.valueOf(128)).intValue();
        Color rgb = Color.rgb(red, green, blue);
        String randomColor = rgb.toString().replace("0x", "#");
        setStyle("-fx-background-color: " + randomColor);

        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()))
                .append(" onData: ").append(id).append("\n");
        list.add(sb.toString());

        Collections.sort(list);
        info.setText(list.toString().replace(", ", "")
                .replace("[", "")
                .replace("]", ""));
        UIScheduler.run(() -> {
            setStyle("-fx-background-color: #EEEEEE");
        }).after(2000);
    }

    public void setOnAction(EventHandler<ActionEvent> value) {
        button.setOnAction(value);
    }

    public void setDefaultButton(boolean value) {
        button.setDefaultButton(value);
    }

    public void onStateChange(ServiceNode.State networkServiceState) {
        button.setText(getTitle() + " [" + networkServiceState.name() + "]");
    }

    public void reset() {
        setStyle("-fx-background-color: #FFFFFF");
        info.setText("");
        list.clear();
    }
}