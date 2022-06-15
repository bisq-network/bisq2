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

package bisq.wallets.electrum.notifications;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static bisq.wallets.electrum.notifications.ElectrumNotifyApi.ENDPOINT_NAME;

@Slf4j
@Path("/" + ENDPOINT_NAME)
public class ElectrumNotifyApi {

    public static final String ENDPOINT_NAME = "electrum-notify";
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public interface Listener {

        void onAddressStatusChanged(String address, String status);

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String notifyEndpoint(ElectrumNotifyRequest request) {
        log.info(request.toString());
        listeners.forEach(listener -> listener.onAddressStatusChanged(request.getAddress(), request.getStatus()));
        return "SUCCESS";
    }

    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

}
