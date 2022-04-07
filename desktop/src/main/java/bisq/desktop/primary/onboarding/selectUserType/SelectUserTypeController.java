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

package bisq.desktop.primary.onboarding.selectUserType;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeController implements Controller {
    private final SelectUserTypeModel model;
    @Getter
    private final SelectUserTypeView view;

    public SelectUserTypeController(DefaultApplicationService applicationService) {
        model = new SelectUserTypeModel(applicationService.getUserProfileService());
        view = new SelectUserTypeView(model, this);

        model.getUserTypes().addAll(SelectUserTypeModel.Type.NEWBIE, SelectUserTypeModel.Type.PRO_TRADER);
    }

    @Override
    public void onActivate() {
        onSelect(SelectUserTypeModel.Type.NEWBIE);
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(SelectUserTypeModel.Type selectedType) {
        model.setSelectedType(selectedType);
        switch (selectedType) {
            case NEWBIE -> {
                model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.newbie.info"));
                model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.newbie.button").toUpperCase());
            }
            case PRO_TRADER -> {
                model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.proTrader.info"));
                model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.proTrader.button").toUpperCase());
            }
        }
    }

    public void onAction() {
        switch (model.getSelectedType()) {
            case NEWBIE -> {
                Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
            }
            case PRO_TRADER -> {
                Navigation.navigateTo(NavigationTarget.ONBOARD_PRO_TRADER);
            }
        }
    }
}
