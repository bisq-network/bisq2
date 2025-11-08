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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class DifficultyAdjustmentController implements Controller {
    @Getter
    private final DifficultyAdjustmentView view;
    private final DifficultyAdjustmentModel model;
    private final UserIdentityService userIdentityService;
    private final SecurityManagerService securityManagerService;
    private final DifficultyAdjustmentService difficultyAdjustmentService;
    private Pin difficultyAdjustmentListItemsPin;
    private Subscription difficultyAdjustmentPin;

    public DifficultyAdjustmentController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        difficultyAdjustmentService = serviceProvider.getBondedRolesService().getDifficultyAdjustmentService();
        model = new DifficultyAdjustmentModel();
        view = new DifficultyAdjustmentView(model, this);
    }

    @Override
    public void onActivate() {
        difficultyAdjustmentListItemsPin = FxBindings.<AuthorizedDifficultyAdjustmentData, DifficultyAdjustmentView.DifficultyAdjustmentListItem>bind(model.getDifficultyAdjustmentListItems())
                .map(DifficultyAdjustmentView.DifficultyAdjustmentListItem::new)
                .to(difficultyAdjustmentService.getAuthorizedDifficultyAdjustmentDataSet());

        double difficultyAdjustmentFactor = difficultyAdjustmentService.getMostRecentValueOrDefault().get();
        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentFactor);
        difficultyAdjustmentPin = EasyBind.subscribe(model.getDifficultyAdjustmentFactor(), factor ->
                model.getDifficultyAdjustmentFactorButtonDisabled().set(factor == null ||
                        !isValidDifficultyAdjustmentFactor(factor.doubleValue())));

        if (difficultyAdjustmentFactor != NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT) {
            securityManagerService.publishDifficultyAdjustment(difficultyAdjustmentFactor);
        }
    }

    @Override
    public void onDeactivate() {
        difficultyAdjustmentListItemsPin.unbind();
        difficultyAdjustmentPin.unsubscribe();
    }

    void onPublishDifficultyAdjustmentFactor() {
        double difficultyAdjustmentFactor = model.getDifficultyAdjustmentFactor().get();
        if (isValidDifficultyAdjustmentFactor(difficultyAdjustmentFactor)) {
            securityManagerService.publishDifficultyAdjustment(difficultyAdjustmentFactor)
                    .whenComplete((result, throwable) -> UIThread.run(() -> {
                        if (throwable != null) {
                            new Popup().error(throwable).show();
                        } else {
                            model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
                        }
                    }));
        }
    }

    void onRemoveDifficultyAdjustmentListItem(DifficultyAdjustmentView.DifficultyAdjustmentListItem item) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        securityManagerService.removeDifficultyAdjustment(item.getData(), userIdentity.getNetworkIdWithKeyPair().getKeyPair());
        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
    }


    boolean isRemoveDifficultyAdjustmentButtonVisible(AuthorizedDifficultyAdjustmentData data) {
        return userIdentityService.getSelectedUserIdentity().getId().equals(data.getSecurityManagerProfileId());
    }

    private static boolean isValidDifficultyAdjustmentFactor(double difficultyAdjustmentFactor) {
        return difficultyAdjustmentFactor >= 0 && difficultyAdjustmentFactor <= NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT;
    }
}
