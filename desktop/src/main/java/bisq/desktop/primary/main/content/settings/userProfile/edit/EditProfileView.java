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

package bisq.desktop.primary.main.content.settings.userProfile.edit;

import bisq.desktop.primary.main.content.settings.userProfile.create.step2.GenerateNewProfileStep2View;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EditProfileView extends GenerateNewProfileStep2View {

    public EditProfileView(EditProfileModel model, EditProfileController controller) {
        super(model, controller);
        
        headLineLabel.setText(Res.get("userProfile.edit.headline"));
        root.requestFocus();
    }
}
