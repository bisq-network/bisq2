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

package bisq.desktop.main.content.user.profile_card.details;

import bisq.desktop.common.view.Model;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class ProfileCardDetailsModel implements Model {
    @Setter
    private UserProfile userProfile;
    @Setter
    private String nickName;
    @Setter
    private String botId;
    @Setter
    private String userId;
    @Setter
    private String transportAddress;
    @Setter
    private String profileAge;
    @Setter
    private String version;
    @Setter
    private Optional<String> statement = Optional.empty();

    private final StringProperty totalReputationScore = new SimpleStringProperty();
}
