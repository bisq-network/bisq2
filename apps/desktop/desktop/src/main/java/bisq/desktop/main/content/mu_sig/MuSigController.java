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

package bisq.desktop.main.content.mu_sig;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigOfferbookController;
import bisq.desktop.main.content.mu_sig.onboarding.MuSigOnboardingController;
import lombok.Getter;

import java.util.Optional;

public class MuSigController extends ContentTabController<MuSigModel> {
    @Getter
    private final MuSigView view;

    public MuSigController(ServiceProvider serviceProvider) {
        super(new MuSigModel(), NavigationTarget.BISQ_MUSIG, serviceProvider);

        view = new MuSigView(model, this);
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case BISQ_MUSIG_ONBOARDING -> Optional.of(new MuSigOnboardingController(serviceProvider));
            case BISQ_MUSIG_OFFERBOOK -> Optional.of(new MuSigOfferbookController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
