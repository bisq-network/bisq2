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

package bisq.desktop.main.content.user.profile_card;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.TabModel;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ProfileCardModel extends TabModel {
    @Setter
    private UserProfile userProfile;
    private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
    private final BooleanProperty ignoreUserSelected = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowUserActionsMenu = new SimpleBooleanProperty();
    private final StringProperty offersTabButtonText = new SimpleStringProperty();

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.PROFILE_CARD_DETAILS;
    }
}
