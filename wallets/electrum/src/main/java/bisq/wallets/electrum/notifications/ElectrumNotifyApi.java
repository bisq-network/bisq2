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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Path("/electrum-notify")
public class ElectrumNotifyApi {

    public static final String ENDPOINT_NAME = "electrum-notify";

    public interface Listener {
        void onAddressStatusChanged(String address, String status);
    }

    private static final CopyOnWriteArrayList<Listener> sListeners = new CopyOnWriteArrayList<>();

    @POST
    @Consumes("application/json")
    public Response notifyEndpoint(@Parameter(required = true) ElectrumNotifyRequest request) {
        log.info(request.toString());
        sListeners.forEach(listener -> listener.onAddressStatusChanged(request.getAddress(), request.getStatus()));
        return Response.ok().entity("SUCCESS").build();
    }

    public static void registerListener(Listener listener) {
        sListeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        sListeners.remove(listener);
    }

}
