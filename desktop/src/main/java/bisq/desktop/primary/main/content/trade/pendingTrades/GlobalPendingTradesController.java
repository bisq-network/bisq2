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

package bisq.desktop.primary.main.content.trade.pendingTrades;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.InitWithDataController;
import bisq.protocol.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalPendingTradesController implements InitWithDataController<GlobalPendingTradesController.InitData> {
    public static record InitData(TakerProtocol<TakerProtocolModel> protocol) {
    }

    private final GlobalPendingTradesModel model;
    @Getter
    private final GlobalPendingTradesView view;
    private final ProtocolService protocolService;
    private Pin protocolsPin;


    public GlobalPendingTradesController(DefaultApplicationService applicationService) {
        model = new GlobalPendingTradesModel();
        view = new GlobalPendingTradesView(model, this);

        protocolService = applicationService.getProtocolService();
        model.filteredItems.setPredicate(e -> e.getProtocol().isPending());
    }

    @Override
    public void initWithData(InitData initData) {
    }

    @Override
    public void onActivate() {
        protocolsPin = FxBindings.<Protocol<? extends ProtocolModel>, GlobalPendingTradeListItem>bind(model.getListItems())
                .map(GlobalPendingTradeListItem::new)
                .to(protocolService.getProtocols());
    }

    @Override
    public void onDeactivate() {
        protocolsPin.unbind();
    }
}
