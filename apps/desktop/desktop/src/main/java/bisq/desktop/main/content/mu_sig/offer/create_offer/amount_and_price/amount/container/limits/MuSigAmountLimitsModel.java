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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.limits;

import bisq.desktop.common.view.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class MuSigAmountLimitsModel implements Model {
    private final String minInUsd;
    private final String maxInUsd;
    private final StringProperty min = new SimpleStringProperty();
    private final StringProperty max = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();

    public MuSigAmountLimitsModel(String minInUsd, String maxInUsd) {
        this.minInUsd = minInUsd;
        this.maxInUsd = maxInUsd;
    }
}