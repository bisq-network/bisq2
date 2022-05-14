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

package bisq.desktop.primary.main.content.newProfilePopup.selectUserType;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.newProfilePopup.NewProfilePopupModel;
import bisq.i18n.Res;
import bisq.social.user.ChatUserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeController implements Controller {
    private final SelectUserTypeModel model;
    @Getter
    private final SelectUserTypeView view;
    private final NewProfilePopupModel popupModel;

    public SelectUserTypeController(DefaultApplicationService applicationService, NewProfilePopupModel popupModel) {
        this.popupModel = popupModel;
        ChatUserIdentity chatUserIdentity = applicationService.getChatUserService().getSelectedUserProfile().get();
        String profileId = chatUserIdentity.getProfileId();
        model = new SelectUserTypeModel(profileId, RoboHash.getImage(new ByteArray(chatUserIdentity.getPubKeyHash())));
        model.getUserTypes().addAll(SelectUserTypeModel.Type.NEWBIE, SelectUserTypeModel.Type.PRO_TRADER);
        view = new SelectUserTypeView(model, this);
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
        if (selectedType != null) {
            switch (selectedType) {
                case NEWBIE -> {
                    model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.newbie.info"));
                    model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.newbie.button"));
                }
                case PRO_TRADER -> {
                    model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.proTrader.info"));
                    model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.proTrader.button"));
                }
            }
        }
    }

    public void onAction() {
        popupModel.increaseStep();
    }

    void onGoBack() {
        popupModel.decreaseStep();
    }
}
