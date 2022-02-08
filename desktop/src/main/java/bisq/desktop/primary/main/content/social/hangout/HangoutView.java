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

package bisq.desktop.primary.main.content.social.hangout;

import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HangoutView extends View<HBox, HangoutModel, HangoutController> {

    public HangoutView(HangoutModel model, HangoutController controller, Pane userProfile) {
        super(new HBox(), model, controller);

        root.setSpacing(Layout.SPACING);
        root.setPadding(new Insets(20, 20, 20, 0));
     
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
