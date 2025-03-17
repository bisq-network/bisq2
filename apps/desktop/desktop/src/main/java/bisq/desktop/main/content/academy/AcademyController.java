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

package bisq.desktop.main.content.academy;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.academy.bisq.BisqAcademyController;
import bisq.desktop.main.content.academy.bitcoin.BitcoinAcademyController;
import bisq.desktop.main.content.academy.foss.FossAcademyController;
import bisq.desktop.main.content.academy.overview.OverviewAcademyController;
import bisq.desktop.main.content.academy.privacy.PrivacyAcademyController;
import bisq.desktop.main.content.academy.security.SecurityAcademyController;
import bisq.desktop.main.content.academy.wallets.WalletsAcademyController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AcademyController extends ContentTabController<AcademyModel> {
    @Getter
    private final AcademyView view;

    public AcademyController(ServiceProvider serviceProvider) {
        super(new AcademyModel(), NavigationTarget.ACADEMY, serviceProvider);

        view = new AcademyView(model, this);
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case OVERVIEW_ACADEMY -> Optional.of(new OverviewAcademyController(serviceProvider));
            case BISQ_ACADEMY -> Optional.of(new BisqAcademyController(serviceProvider));
            case BITCOIN_ACADEMY -> Optional.of(new BitcoinAcademyController(serviceProvider));
            case WALLETS_ACADEMY -> Optional.of(new WalletsAcademyController(serviceProvider));
            case SECURITY_ACADEMY -> Optional.of(new SecurityAcademyController(serviceProvider));
            case PRIVACY_ACADEMY -> Optional.of(new PrivacyAcademyController(serviceProvider));
            case FOSS_ACADEMY -> Optional.of(new FossAcademyController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
