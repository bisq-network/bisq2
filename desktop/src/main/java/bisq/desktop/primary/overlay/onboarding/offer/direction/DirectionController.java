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

package bisq.desktop.primary.overlay.onboarding.offer.direction;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.offer.spec.Direction;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectionController implements Controller {
    private final DirectionModel model;
    @Getter
    private final DirectionView view;

    public DirectionController(DefaultApplicationService applicationService) {
        model = new DirectionModel();
        view = new DirectionView(model, this);
        onSelect(Direction.BUY);
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(Direction selectedDirection) {
        model.getDirection().set(selectedDirection);
    }
}
