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

package bisq.desktop.main.content.user.user_profile.create.step1;

import bisq.desktop.overlay.onboarding.create_profile.CreateProfileView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateNewProfileStep1View extends CreateProfileView {

    public CreateNewProfileStep1View(CreateNewProfileStep1Model model, CreateNewProfileStep1Controller controller) {
        super(model, controller);

        createProfileButton.setText(Res.get("action.next"));

        root.setPadding(new Insets(-30, 0, 10, 0));
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
