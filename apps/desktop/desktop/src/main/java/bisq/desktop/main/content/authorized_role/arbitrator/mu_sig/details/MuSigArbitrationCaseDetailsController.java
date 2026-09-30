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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.details;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.MuSigArbitrationCaseListItem;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationCaseDetailSection;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationCaseMediationResultSection;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationCaseOverviewSection;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationResultSection;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.support.arbitration.ArbitrationCaseState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigArbitrationCaseDetailsController
        extends NavigationController
        implements InitWithDataController<MuSigArbitrationCaseDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigArbitrationCaseListItem muSigArbitrationCaseListItem;

        public InitData(MuSigArbitrationCaseListItem muSigArbitrationCaseListItem) {
            this.muSigArbitrationCaseListItem = muSigArbitrationCaseListItem;
        }
    }

    @Getter
    private final MuSigArbitrationCaseDetailsModel model;
    @Getter
    private final MuSigArbitrationCaseDetailsView view;

    private final ServiceProvider serviceProvider;
    private final MuSigArbitrationCaseOverviewSection muSigArbitrationCaseOverviewSection;
    private final MuSigArbitrationCaseDetailSection muSigArbitrationCaseDetailSection;
    private final MuSigArbitrationCaseMediationResultSection muSigArbitrationCaseMediationResultSection;

    public MuSigArbitrationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_ARBITRATION_CASE_DETAILS);
        this.serviceProvider = serviceProvider;

        muSigArbitrationCaseOverviewSection = new MuSigArbitrationCaseOverviewSection(serviceProvider, false);
        muSigArbitrationCaseDetailSection = new MuSigArbitrationCaseDetailSection(false);
        muSigArbitrationCaseMediationResultSection = new MuSigArbitrationCaseMediationResultSection();

        model = new MuSigArbitrationCaseDetailsModel();
        view = new MuSigArbitrationCaseDetailsView(
                model,
                this,
                muSigArbitrationCaseOverviewSection.getRoot(),
                muSigArbitrationCaseDetailSection.getRoot(),
                muSigArbitrationCaseMediationResultSection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMuSigArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        muSigArbitrationCaseOverviewSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        muSigArbitrationCaseDetailSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        muSigArbitrationCaseMediationResultSection.setMediationResult(initData.muSigArbitrationCaseListItem
                .getMuSigArbitrationCase().getMuSigArbitrationRequest().getMuSigMediationResult());
        muSigArbitrationCaseMediationResultSection.setMediator(initData.muSigArbitrationCaseListItem
                .getMuSigArbitrationCase().getMuSigArbitrationRequest().getContract().getMediator().orElseThrow());

        boolean isClosed = initData.muSigArbitrationCaseListItem
                .getMuSigArbitrationCase()
                .arbitrationCaseStateObservable()
                .get() == ArbitrationCaseState.CLOSED;
        if (isClosed) {
            MuSigArbitrationResultSection arbitrationResultSection = new MuSigArbitrationResultSection(serviceProvider);
            arbitrationResultSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
            view.setArbitrationResultComponent(Optional.of(arbitrationResultSection.getRoot()));
        } else {
            view.setArbitrationResultComponent(Optional.empty());
        }
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
