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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.direction;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.Direction;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class CreateOfferDirectionController implements Controller {
    private final CreateOfferDirectionModel model;
    @Getter
    private final CreateOfferDirectionView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;

    public CreateOfferDirectionController(ServiceProvider serviceProvider,
                                          Runnable onNextHandler,
                                          Consumer<Boolean> mainButtonsVisibleHandler) {
        this.onNextHandler = onNextHandler;
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;

        model = new CreateOfferDirectionModel();
        view = new CreateOfferDirectionView(model, this);
        setDirection(Direction.BUY);
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
    }

    @Override
    public void onDeactivate() {
    }

    void onSelectDirection(Direction direction) {
        setDirection(direction);
        if (direction == Direction.BUY) {
            model.getBuyButtonDisabled().set(true);
            onNextHandler.run();
        }
    }

    void onCloseReputationInfo() {
        setDirection(Direction.BUY);
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
        boolean showReputationInfo = !model.getIgnoreShowReputationInfo().get() && direction == Direction.SELL;
        mainButtonsVisibleHandler.accept(!showReputationInfo);
        model.getShowReputationInfo().set(showReputationInfo);
    }
}
