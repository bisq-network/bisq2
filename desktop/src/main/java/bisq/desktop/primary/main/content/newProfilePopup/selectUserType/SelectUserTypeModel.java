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

package bisq.desktop.primary.main.content.newProfilePopup.selectUserType;

import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SelectUserTypeModel implements Model {
    public enum Type {
        NEWBIE(Res.get("satoshisquareapp.selectTraderType.newbie")),
        PRO_TRADER(Res.get("satoshisquareapp.selectTraderType.proTrader"));

        private final String displayString;

        Type(String displayString) {
            this.displayString = displayString;
        }

        @Override
        public String toString() {
            return displayString;
        }
    }

    private final ObservableList<Type> userTypes = FXCollections.observableArrayList();
    private final StringProperty info = new SimpleStringProperty();
    private final StringProperty buttonText = new SimpleStringProperty();
    @Setter
    private Type selectedType;
    private final String profileId;
    private final Image roboHashNode;

    public SelectUserTypeModel(String profileId, Image roboHashNode) {
        this.profileId = profileId;
        this.roboHashNode = roboHashNode;
    }
}