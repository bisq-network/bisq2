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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.details;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigMediationCaseDetailsController extends NavigationController implements InitWithDataController<MuSigMediationCaseDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigMediationCaseListItem muSigMediationCaseListItem;

        public InitData(MuSigMediationCaseListItem muSigMediationCaseListItem) {
            this.muSigMediationCaseListItem = muSigMediationCaseListItem;
        }
    }

    @Getter
    private final MuSigMediationCaseDetailsModel model;
    @Getter
    private final MuSigMediationCaseDetailsView view;

    private final MuSigMediationCaseOverviewSection muSigMediationCaseOverviewSection;
    private final MuSigMediationCaseDetailSection muSigMediationCaseDetailSection;

    public MuSigMediationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_MEDIATION_CASE_DETAILS);

        muSigMediationCaseOverviewSection = new MuSigMediationCaseOverviewSection(serviceProvider);
        muSigMediationCaseDetailSection = new MuSigMediationCaseDetailSection(serviceProvider);

        model = new MuSigMediationCaseDetailsModel();
        view = new MuSigMediationCaseDetailsView(
                model,
                this,
                muSigMediationCaseOverviewSection.getRoot(),
                muSigMediationCaseDetailSection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMuSigMediationCaseListItem(initData.muSigMediationCaseListItem);
        muSigMediationCaseOverviewSection.setMediationCaseListItem(initData.muSigMediationCaseListItem);
        muSigMediationCaseDetailSection.setMediationCaseListItem(initData.muSigMediationCaseListItem);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    void onClose() {
        OverlayController.hide();
    }
}
