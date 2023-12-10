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

package bisq.desktop.main.content.user.reputation.signedAccount;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.user.reputation.signedAccount.tab1.SignedWitnessTab1Controller;
import bisq.desktop.main.content.user.reputation.signedAccount.tab2.SignedWitnessTab2Controller;
import bisq.desktop.main.content.user.reputation.signedAccount.tab3.SignedWitnessTab3Controller;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SignedWitnessController extends TabController<SignedWitnessModel> {
    @Getter
    private final SignedWitnessView view;
    private final ServiceProvider serviceProvider;

    public SignedWitnessController(ServiceProvider serviceProvider) {
        super(new SignedWitnessModel(), NavigationTarget.SIGNED_WITNESS);

        this.serviceProvider = serviceProvider;
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
                return Optional.of(new SignedWitnessTab1Controller(serviceProvider));
            }
            case SIGNED_WITNESS_TAB_2: {
                return Optional.of(new SignedWitnessTab2Controller(serviceProvider));
            }
            case SIGNED_WITNESS_TAB_3: {
                return Optional.of(new SignedWitnessTab3Controller(serviceProvider, view));
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
