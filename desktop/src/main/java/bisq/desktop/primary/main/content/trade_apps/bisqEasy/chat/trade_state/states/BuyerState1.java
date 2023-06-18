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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.BisqText;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState1 extends BaseState {
    private final Controller controller;

    public BuyerState1(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, UserIdentity myUserIdentity) {
        controller = new Controller(applicationService, bisqEasyOffer, myUserIdentity);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, UserIdentity myUserIdentity) {
            super(applicationService, bisqEasyOffer, myUserIdentity);
        }

        @Override
        protected Model createModel() {
            return new Model();
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
    }

    public static class View extends BaseState.View<Model, Controller> {

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase1.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase1.info")));
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
        }
    }
}