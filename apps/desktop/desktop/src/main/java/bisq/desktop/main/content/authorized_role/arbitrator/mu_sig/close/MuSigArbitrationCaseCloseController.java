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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.close;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.MuSigArbitrationCaseListItem;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationCaseDetailSection;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationCaseOverviewSection;
import bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.components.MuSigArbitrationResultSection;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigArbitrationCaseCloseController extends NavigationController implements InitWithDataController<MuSigArbitrationCaseCloseController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigArbitrationCaseListItem muSigArbitrationCaseListItem;
        private final Runnable onCloseHandler;

        public InitData(MuSigArbitrationCaseListItem muSigArbitrationCaseListItem, Runnable onCloseHandler) {
            this.muSigArbitrationCaseListItem = muSigArbitrationCaseListItem;
            this.onCloseHandler = onCloseHandler;
        }
    }

    private Runnable onCloseHandler;

    @Getter
    private final MuSigArbitrationCaseCloseModel model;
    @Getter
    private final MuSigArbitrationCaseCloseView view;

    private final MuSigArbitrationCaseOverviewSection muSigArbitrationCaseOverviewSection;
    private final MuSigArbitrationCaseDetailSection muSigArbitrationCaseDetailSection;
    private final MuSigArbitrationResultSection muSigArbitrationResultSection;

    public MuSigArbitrationCaseCloseController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_ARBITRATION_CASE_CLOSE);

        muSigArbitrationCaseOverviewSection = new MuSigArbitrationCaseOverviewSection(serviceProvider, true);
        muSigArbitrationCaseDetailSection = new MuSigArbitrationCaseDetailSection(true);
        muSigArbitrationResultSection = new MuSigArbitrationResultSection(serviceProvider);

        model = new MuSigArbitrationCaseCloseModel();
        view = new MuSigArbitrationCaseCloseView(
                model,
                this,
                muSigArbitrationCaseOverviewSection.getRoot(),
                muSigArbitrationCaseDetailSection.getRoot(),
                muSigArbitrationResultSection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMuSigArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        onCloseHandler = initData.onCloseHandler;
        muSigArbitrationCaseOverviewSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        muSigArbitrationCaseDetailSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
        muSigArbitrationResultSection.setArbitrationCaseListItem(initData.muSigArbitrationCaseListItem);
    }

    @Override
    public void onActivate() {
        model.getCloseCaseButtonDisabled().bind(muSigArbitrationResultSection.hasRequiredSelectionsProperty().not());
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
        muSigArbitrationResultSection.closeCase();
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }
        OverlayController.hide();
    }
}
