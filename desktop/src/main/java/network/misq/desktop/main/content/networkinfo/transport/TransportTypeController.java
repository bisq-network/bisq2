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

package network.misq.desktop.main.content.networkinfo.transport;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;

public class TransportTypeController implements Controller {
    private final TransportTypeModel model;
    @Getter
    private final TransportTypeView view;
    private final DefaultServiceProvider serviceProvider;

    public TransportTypeController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new TransportTypeModel(serviceProvider);
        view = new TransportTypeView(model, this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

}
