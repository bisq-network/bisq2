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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.tab1.TradeGuideTab1Controller;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.tab2.TradeGuideTab2Controller;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.tab3.TradeGuideTab3Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeGuideController extends TabController<TradeGuideModel> {
    @Getter
    private final TradeGuideView view;
    private final DefaultApplicationService applicationService;

    public TradeGuideController(DefaultApplicationService applicationService) {
        super(new TradeGuideModel(), NavigationTarget.TRADE_GUIDE);

        this.applicationService = applicationService;
        view = new TradeGuideView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
/*

    @Override
    public boolean useCaching() {
        return false;
    }
*/

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_GUIDE_TAB_1: {
                return Optional.of(new TradeGuideTab1Controller(applicationService));
            }
            case TRADE_GUIDE_TAB_2: {
                return Optional.of(new TradeGuideTab2Controller(applicationService));
            }
            case TRADE_GUIDE_TAB_3: {
                return Optional.of(new TradeGuideTab3Controller(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onExpand() {
        model.getIsCollapsed().set(false);
    }

    public void onCollapse() {
        model.getIsCollapsed().set(true);
    }
}
