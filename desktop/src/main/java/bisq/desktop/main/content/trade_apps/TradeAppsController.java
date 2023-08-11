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

package bisq.desktop.main.content.trade_apps;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.trade_apps.more.MoreProtocolsController;
import bisq.desktop.main.content.trade_apps.overview.TradeOverviewController;
import bisq.desktop.main.content.trade_apps.roadmap.ProtocolRoadmapController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeAppsController extends TabController<TradeAppsModel> {
    private final ServiceProvider serviceProvider;
    @Getter
    private final TradeAppsView view;

    public TradeAppsController(ServiceProvider serviceProvider) {
        super(new TradeAppsModel(), NavigationTarget.TRADE_PROTOCOLS);

        this.serviceProvider = serviceProvider;

        view = new TradeAppsView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        if (!model.getMoreTabVisible().get()) {
            model.getMoreTabVisible().set(navigationTarget == NavigationTarget.MORE_TRADE_PROTOCOLS);
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_PROTOCOLS_OVERVIEW: {
                return Optional.of(new TradeOverviewController(serviceProvider));
            }
            case BISQ_EASY_INFO: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.BISQ_EASY,
                        "protocol-multisig",
                        "https://bisq.network/"));
            }
            case MULTISIG: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.MULTISIG,
                        "protocol-multisig",
                        "https://bisq.network/"));
            }
            case SUBMARINE: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.SUBMARINE,
                        "protocol-submarine",
                        "https://docs.lightning.engineering/the-lightning-network/multihop-payments/understanding-submarine-swaps"));
            }
            case LIGHTNING_FIAT: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIGHTNING_FIAT,
                        "protocol-lightning-fiat",
                        "https://bisq.wiki/ln-liquid"));
            }
            case MORE_TRADE_PROTOCOLS: {
                return Optional.of(new MoreProtocolsController());
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
