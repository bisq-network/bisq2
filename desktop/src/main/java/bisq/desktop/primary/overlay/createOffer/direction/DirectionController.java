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

package bisq.desktop.primary.overlay.createOffer.direction;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.spec.Direction;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class DirectionController implements Controller {
    private final DirectionModel model;
    @Getter
    private final DirectionView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> buttonsVisibleHandler;

    public DirectionController(DefaultApplicationService applicationService,
                               Runnable onNextHandler,
                               Consumer<Boolean> buttonsVisibleHandler) {
        this.onNextHandler = onNextHandler;
        this.buttonsVisibleHandler = buttonsVisibleHandler;

        model = new DirectionModel();
        view = new DirectionView(model, this);
        setDirection(Direction.BUY);
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    @Override
    public void onActivate() {
        setDirection(Direction.BUY);
    }

    @Override
    public void onDeactivate() {
    }


    void onSelectDirection(Direction direction) {
        setDirection(direction);
    }

    void onCloseReputationInfo() {
        setDirection(Direction.BUY);
    }

    void onGainReputation() {
        //model.getIgnoreShowReputationInfo().set(true);
        setDirection(Direction.BUY);
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.REPUTATION);
    }

    void onIgnoreReputation() {
        // model.getIgnoreShowReputationInfo().set(true);
        onNextHandler.run();
    }

    private void setDirection(Direction direction) {
        model.getDirection().set(direction);
        boolean showReputationInfo = !model.getIgnoreShowReputationInfo().get() && direction == Direction.SELL;
        buttonsVisibleHandler.accept(!showReputationInfo);
        model.getShowReputationInfo().set(showReputationInfo);
    }
}
