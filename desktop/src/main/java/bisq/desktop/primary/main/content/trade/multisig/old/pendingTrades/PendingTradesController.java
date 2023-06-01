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

package bisq.desktop.primary.main.content.trade.multisig.old.pendingTrades;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.InitWithDataController;
import bisq.protocol.poc.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PendingTradesController implements InitWithDataController<PendingTradesController.InitData> {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class InitData {
        private final TakerProtocol<TakerProtocolModel> protocol;

        public InitData(TakerProtocol<TakerProtocolModel> protocol) {
            this.protocol = protocol;
        }

    }

    private final PendingTradesModel model;
    @Getter
    private final PendingTradesView view;
    private final PocProtocolService pocProtocolService;
    private Pin protocolsPin;


    public PendingTradesController(DefaultApplicationService applicationService) {
        model = new PendingTradesModel();
        view = new PendingTradesView(model, this);

        pocProtocolService = applicationService.getPocProtocolService();
        model.filteredItems.setPredicate(e -> e.getProtocol().isPending());
    }

    @Override
    public void initWithData(InitData initData) {
    }

    @Override
    public void onActivate() {
        protocolsPin = FxBindings.<Protocol<? extends ProtocolModel>, PendingTradeListItem>bind(model.getListItems())
                .map(PendingTradeListItem::new)
                .to(pocProtocolService.getProtocols());
    }

    @Override
    public void onDeactivate() {
        protocolsPin.unbind();
    }
}
