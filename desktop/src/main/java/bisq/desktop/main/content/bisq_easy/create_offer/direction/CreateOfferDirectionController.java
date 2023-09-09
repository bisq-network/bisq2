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

package bisq.desktop.main.content.bisq_easy.create_offer.direction;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.offer.Direction;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class CreateOfferDirectionController implements Controller {
    private final CreateOfferDirectionModel model;
    @Getter
    private final CreateOfferDirectionView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;

    public CreateOfferDirectionController(ServiceProvider serviceProvider,
                                          Runnable onNextHandler,
                                          Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.onNextHandler = onNextHandler;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();

        model = new CreateOfferDirectionModel();
        view = new CreateOfferDirectionView(model, this);
        setDirection(Direction.BUY);
        applyShowReputationInfo();
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        setDirection(Direction.BUY);
        applyShowReputationInfo();
    }

    @Override
    public void onDeactivate() {
    }

    void onSelectDirection(Direction direction) {
        setDirection(direction);
        applyShowReputationInfo();
        if (direction == Direction.BUY && !model.getShowReputationInfo().get()) {
            onNextHandler.run();
        }
    }

    void onCloseReputationInfo() {
        setDirection(Direction.BUY);
        applyShowReputationInfo();
    }

    void onGainReputation() {
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.REPUTATION);
    }

    void onIgnoreReputation() {
        onNextHandler.run();
    }

    private void setDirection(Direction direction) {
        model.getDirection().set(direction);
    }

    private void applyShowReputationInfo() {
        if (model.getDirection().get() == Direction.BUY) {
            model.getShowReputationInfo().set(false);
            navigationButtonsVisibleHandler.accept(true);
            return;
        }

        ReputationScore reputationScore = reputationService.getReputationScore(checkNotNull(userIdentityService.getSelectedUserIdentity()).getUserProfile());
        if (!reputationScore.hasReputation()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getShowReputationInfo().set(true);
        }
    }
}
