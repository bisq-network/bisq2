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

package bisq.desktop.primary.main.content.portfolio.pending;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.InitWithDataController;
import bisq.offer.Offer;
import javafx.beans.property.BooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PendingTradesController implements InitWithDataController<PendingTradesController.InitData> {

    public static record InitData(Offer offer, BooleanProperty showTakeOfferTab) {
    }

    private final PendingTradesModel model;
    @Getter
    private final PendingTradesView view;


    public PendingTradesController(DefaultApplicationService applicationService) {
        model = new PendingTradesModel();
        view = new PendingTradesView(model, this);
    }

    @Override
    public void initWithData(InitData initData) {
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    public void onViewDetached() {
    }
}
