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

package bisq.desktop.primary.main.content.user.user_profile.create.step1;

import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.primary.overlay.onboarding.create_profile.CreateProfileView;
import bisq.i18n.Res;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateNewProfileStep1View extends CreateProfileView {

    private final CreateNewProfileStep1Controller createNewProfileStep1Controller;
    private Scene rootScene;

    public CreateNewProfileStep1View(CreateNewProfileStep1Model model, CreateNewProfileStep1Controller controller) {
        super(model, controller);

        this.createNewProfileStep1Controller = controller;

        createProfileButton.setText(Res.get("action.next"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, createNewProfileStep1Controller::onQuit);
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, createNewProfileStep1Controller::onCancel);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        
        if (rootScene != null) {
            rootScene.setOnKeyReleased(null);
        }
    }
}
