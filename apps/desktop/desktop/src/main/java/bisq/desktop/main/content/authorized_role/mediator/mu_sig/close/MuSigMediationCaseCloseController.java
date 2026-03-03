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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.close;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailSection;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseOverviewSection;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationResultSection;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigMediationCaseCloseController extends NavigationController implements InitWithDataController<MuSigMediationCaseCloseController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigMediationCaseListItem muSigMediationCaseListItem;
        private final Runnable onCloseHandler;

        public InitData(MuSigMediationCaseListItem muSigMediationCaseListItem, Runnable onCloseHandler) {
            this.muSigMediationCaseListItem = muSigMediationCaseListItem;
            this.onCloseHandler = onCloseHandler;
        }
    }

    private Runnable onCloseHandler;

    @Getter
    private final MuSigMediationCaseCloseModel model;
    @Getter
    private final MuSigMediationCaseCloseView view;

    private final MuSigMediationCaseOverviewSection muSigMediationCaseOverviewSection;
    private final MuSigMediationCaseDetailSection muSigMediationCaseDetailSection;
    private final MuSigMediationResultSection muSigMediationResultSection;

    public MuSigMediationCaseCloseController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_MEDIATION_CASE_CLOSE);

        muSigMediationCaseOverviewSection = new MuSigMediationCaseOverviewSection(serviceProvider, true);
        muSigMediationCaseDetailSection = new MuSigMediationCaseDetailSection(serviceProvider, true);
        muSigMediationResultSection = new MuSigMediationResultSection(serviceProvider);

        model = new MuSigMediationCaseCloseModel();
        view = new MuSigMediationCaseCloseView(
                model,
                this,
                muSigMediationCaseOverviewSection.getRoot(),
                muSigMediationCaseDetailSection.getRoot(),
                muSigMediationResultSection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMuSigMediationCaseListItem(initData.muSigMediationCaseListItem);
        onCloseHandler = initData.onCloseHandler;
        muSigMediationCaseOverviewSection.setMediationCaseListItem(initData.muSigMediationCaseListItem);
        muSigMediationCaseDetailSection.setMediationCaseListItem(initData.muSigMediationCaseListItem);
        muSigMediationResultSection.setMediationCaseListItem(initData.muSigMediationCaseListItem);
    }

    @Override
    public void onActivate() {
        model.getCloseCaseButtonDisabled().bind(muSigMediationResultSection.hasRequiredSelectionsProperty().not());
    }

    @Override
    public void onDeactivate() {
        model.getCloseCaseButtonDisabled().unbind();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    void onCloseCase() {
        doClose();
    }

    void onClose() {
        OverlayController.hide();
    }

    private void doClose() {
        muSigMediationResultSection.closeCase();
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }
        OverlayController.hide();
    }
}
