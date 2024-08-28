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

package bisq.desktop.main.content.bisq_easy;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyView extends ContentTabView<BisqEasyModel, BisqEasyController> {
    public BisqEasyView(BisqEasyModel model, BisqEasyController controller) {
        super(model, controller);

        addTab(Res.get("bisqEasy.dashboard"), NavigationTarget.BISQ_EASY_ONBOARDING);
        addTab(Res.get("bisqEasy.offerbook"), NavigationTarget.BISQ_EASY_OFFERBOOK);
        TabButton openTrades = addTab(Res.get("bisqEasy.openTrades"), NavigationTarget.BISQ_EASY_OPEN_TRADES);
        openTrades.getNumMessagesBadge().getStyleClass().add("open-trades-badge");
        addTab(Res.get("bisqEasy.privateChat"), NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
    }
}
