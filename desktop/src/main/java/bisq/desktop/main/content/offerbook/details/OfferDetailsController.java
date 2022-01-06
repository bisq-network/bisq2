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

package bisq.desktop.main.content.offerbook.details;

import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.offerbook.OfferListItem;
import javafx.geometry.Bounds;
import lombok.Getter;

public class OfferDetailsController implements Controller {
    private final OfferListItem item;
    private final Bounds boundsInParent;
    @Getter
    private final OfferDetailsView view;
    private final OfferDetailsModel model;

    public OfferDetailsController(OfferListItem item, Bounds boundsInParent) {
        this.item = item;
        this.boundsInParent = boundsInParent;
        model = new OfferDetailsModel(item);
        view = new OfferDetailsView(model, this, boundsInParent);
    }
}
