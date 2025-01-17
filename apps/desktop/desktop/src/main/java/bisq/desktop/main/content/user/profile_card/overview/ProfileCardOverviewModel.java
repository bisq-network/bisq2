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

package bisq.desktop.main.content.user.profile_card.overview;

import bisq.desktop.common.view.Model;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProfileCardOverviewModel implements Model {
    @Setter
    private UserProfile userProfile;
    @Setter
    private String numOffers;
    @Setter
    private String numPublicTextMessages;
    @Setter
    private String totalBaseOfferAmountToBuy;
    @Setter
    private String totalBaseOfferAmountToSell;
    @Setter
    private String profileAge;
    @Setter
    private String statement;
    @Setter
    private String tradeTerms;

    private final StringProperty lastUserActivity = new SimpleStringProperty();
}
