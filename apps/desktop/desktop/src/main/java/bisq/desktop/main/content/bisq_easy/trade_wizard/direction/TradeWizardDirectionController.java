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

package bisq.desktop.main.content.bisq_easy.trade_wizard.direction;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.offer.Direction;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class TradeWizardDirectionController implements Controller {
    private final TradeWizardDirectionModel model;
    @Getter
    private final TradeWizardDirectionView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final ReputationService reputationService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final UserIdentityService userIdentityService;

    public TradeWizardDirectionController(ServiceProvider serviceProvider,
                                          Runnable onNextHandler,
                                          Consumer<Boolean> navigationButtonsVisibleHandler,
                                          Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.onNextHandler = onNextHandler;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        model = new TradeWizardDirectionModel();
        view = new TradeWizardDirectionView(model, this);
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
        closeAndNavigateToHandler.accept(NavigationTarget.REPUTATION);
    }

    void onTradeWithoutReputation() {
        navigationButtonsVisibleHandler.accept(true);
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

        ReputationScore reputationScore = userIdentityService.getSelectedUserIdentity() != null ?
                reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()) :
                ReputationScore.NONE;
        if (!reputationScore.hasReputation()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getShowReputationInfo().set(true);
        }
    }
}
