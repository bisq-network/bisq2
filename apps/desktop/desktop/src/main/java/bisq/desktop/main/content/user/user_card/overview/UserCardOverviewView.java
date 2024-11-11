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

package bisq.desktop.main.content.user.user_card.overview;

import bisq.desktop.common.view.View;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserCardOverviewView extends View<VBox, UserCardOverviewModel, UserCardOverviewController> {
    private final Label statementLabel, tradeTermsLabel;

    public UserCardOverviewView(UserCardOverviewModel model,
                                UserCardOverviewController controller) {
        super(new VBox(), model, controller);

        // Statement
        statementLabel = new Label();

        // Trade terms
        tradeTermsLabel = new Label();
    }

    @Override
    protected void onViewAttached() {

    }

    @Override
    protected void onViewDetached() {

    }
}
