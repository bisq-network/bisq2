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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.overlay.onboarding.create_profile;

import bisq.desktop_ui_harness_app.AbstractDesktopAutomationViewBinder;

public final class CreateProfileAutomationBinder extends AbstractDesktopAutomationViewBinder<CreateProfileView> {
    @Override
    public Class<CreateProfileView> viewType() {
        return CreateProfileView.class;
    }

    @Override
    public void bind(CreateProfileView view) {
        scope(view.getRoot(), "create-profile");
        id(view.nicknameInput(), "nickname");
        id(view.createProfileAction(), "create");
    }
}
