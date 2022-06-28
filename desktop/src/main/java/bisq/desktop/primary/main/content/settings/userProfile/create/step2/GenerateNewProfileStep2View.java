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

package bisq.desktop.primary.main.content.settings.userProfile.create.step2;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateNewProfileStep2View extends View<VBox, GenerateNewProfileStep2Model, GenerateNewProfileStep2Controller> {

    public GenerateNewProfileStep2View(GenerateNewProfileStep2Model model, GenerateNewProfileStep2Controller controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 10, 0));

        Label headLineLabel = new Label(Res.get("generateNym.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("generateNym.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");


       /* nicknameTextInputBox = new TextInputBox(Res.get("addNickName.nickName"),
                Res.get("addNickName.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(300);*/


        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 50, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel
        );
    }

    @Override
    protected void onViewAttached() {
       // nicknameTextInputBox.textProperty().bindBidirectional(model.getNickName());
    }

    @Override
    protected void onViewDetached() {
    }
}
