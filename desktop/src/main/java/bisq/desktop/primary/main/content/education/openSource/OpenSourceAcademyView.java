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

package bisq.desktop.primary.main.content.education.openSource;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenSourceAcademyView extends View<VBox, OpenSourceAcademyModel, OpenSourceAcademyController> {

    public OpenSourceAcademyView(OpenSourceAcademyModel model, OpenSourceAcademyController controller) {
        super(new VBox(), model, controller);

        Text headlineLabel = new Text(Res.get("academy.openSource"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        root.getChildren().add(headlineLabel);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {

    }    
}
