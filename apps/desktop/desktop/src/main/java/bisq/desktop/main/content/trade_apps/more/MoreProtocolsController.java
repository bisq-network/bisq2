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

package bisq.desktop.main.content.trade_apps.more;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.main.content.trade_apps.roadmap.ProtocolRoadmapController;
import lombok.Getter;

import java.util.Optional;

public class MoreProtocolsController implements InitWithDataController<MoreProtocolsController.InitData> {
    @Getter
    public static final class InitData {
        private final TradeProtocolType tradeProtocolType;

        public InitData(TradeProtocolType tradeProtocolType) {
            this.tradeProtocolType = tradeProtocolType;
        }
    }

    @Getter
    private final MoreProtocolsView view;
    private final MoreProtocolsModel model;

    public MoreProtocolsController() {
        model = new MoreProtocolsModel();
        view = new MoreProtocolsView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        if (getProtocolRoadmapController(data.getTradeProtocolType()).isPresent()) {
            ProtocolRoadmapController protocolRoadmapController = getProtocolRoadmapController(data.getTradeProtocolType()).get();
            model.getProtocolRoadmapView().set(protocolRoadmapController.getView());
        }
    }

    public Optional<ProtocolRoadmapController> getProtocolRoadmapController(TradeProtocolType tradeProtocolType) {
        switch (tradeProtocolType) {
            case LIQUID_MU_SIG: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIQUID_MU_SIG,
                        "https://bisq.wiki/Trade_Protocols#Liquid_MuSig"));
            }

            case LIQUID_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIQUID_SWAP,
                        "https://bisq.wiki/Trade_Protocols#Liquid_Swaps"));
            }
            case BSQ_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.BSQ_SWAP,
                        "https://bisq.wiki/BSQ"));
            }
            case LIGHTNING_ESCROW: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIGHTNING_ESCROW,
                        "https://github.com/bisq-network/proposals/issues/416"));
            }
            case MONERO_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.MONERO_SWAP,
                        "https://bisq.wiki/Trade_Protocols#Monero_Swaps"));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
