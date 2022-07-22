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

package bisq.desktop.primary.main.content.trade.multiSig.old.closedTrades;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.protocol.Protocol;
import bisq.protocol.ProtocolModel;
import bisq.protocol.ProtocolService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosedTradesController implements Controller {
    private final ClosedTradesModel model;
    @Getter
    private final ClosedTradesView view;
    private final ProtocolService protocolService;
    private int pin;
    private Pin protocolsPin;


    public ClosedTradesController(DefaultApplicationService applicationService) {
        model = new ClosedTradesModel();
        view = new ClosedTradesView(model, this);
        protocolService = applicationService.getProtocolService();
        model.filteredItems.setPredicate(e -> e.getProtocol().isCompleted());
    }


    @Override
    public void onActivate() {
        protocolsPin = FxBindings.<Protocol<? extends ProtocolModel>, ClosedTradeListItem>bind(model.getListItems())
                .map(ClosedTradeListItem::new)
                .to(protocolService.getProtocols());
    }

    @Override
    public void onDeactivate() {
        protocolsPin.unbind();
    }
}
