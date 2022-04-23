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

package bisq.desktop.primary.main.content.trade.closedTrades;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.CachingController;
import bisq.protocol.Protocol;
import bisq.protocol.ProtocolModel;
import bisq.protocol.ProtocolService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalClosedTradesController implements CachingController {
    private final GlobalClosedTradesModel model;
    @Getter
    private final GlobalClosedTradesView view;
    private final ProtocolService protocolService;
    private Pin protocolsPin;

    public GlobalClosedTradesController(DefaultApplicationService applicationService) {
        model = new GlobalClosedTradesModel();
        view = new GlobalClosedTradesView(model, this);
        protocolService = applicationService.getProtocolService();
        model.filteredItems.setPredicate(e -> e.getProtocol().isCompleted());
    }


    @Override
    public void onActivate() {
        protocolsPin = FxBindings.<Protocol<? extends ProtocolModel>, GlobalClosedTradeListItem>bind(model.getListItems())
                .map(GlobalClosedTradeListItem::new)
                .to(protocolService.getProtocols());
    }

    @Override
    public void onDeactivate() {
        protocolsPin.unbind();
    }
}
