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

package bisq.desktop.common.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

public class StageUtil {
    public static Stage addToOverlayStage(Parent node) {
        return addToOverlayStage(node, Optional.empty());
    }

    public static Stage addToOverlayStage(Parent node, String title) {
        return addToOverlayStage(node, Optional.of(title));
    }

    public static Stage addToOverlayStage(Parent node, Optional<String> title) {
        Scene scene = new Scene(node);
        Stage stage = new Stage();
        title.ifPresent(stage::setTitle);
        stage.setScene(scene);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
        return stage;
    }

}