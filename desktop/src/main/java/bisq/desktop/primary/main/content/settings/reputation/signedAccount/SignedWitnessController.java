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

package bisq.desktop.primary.main.content.settings.reputation.signedAccount;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.reputation.signedAccount.tab1.SignedWitnessTab1Controller;
import bisq.desktop.primary.main.content.settings.reputation.signedAccount.tab2.SignedWitnessTab2Controller;
import bisq.desktop.primary.main.content.settings.reputation.signedAccount.tab3.SignedWitnessTab3Controller;
import bisq.desktop.primary.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SignedWitnessController extends TabController<SignedWitnessModel> {
    @Getter
    private final SignedWitnessView view;
    private final DefaultApplicationService applicationService;

    public SignedWitnessController(DefaultApplicationService applicationService) {
        super(new SignedWitnessModel(), NavigationTarget.SIGNED_WITNESS);

        this.applicationService = applicationService;
        view = new SignedWitnessView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SIGNED_WITNESS_TAB_1: {
                return Optional.of(new SignedWitnessTab1Controller(applicationService));
            }
            case SIGNED_WITNESS_TAB_2: {
                return Optional.of(new SignedWitnessTab2Controller(applicationService));
            }
            case SIGNED_WITNESS_TAB_3: {
                return Optional.of(new SignedWitnessTab3Controller(applicationService, view));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }
}
