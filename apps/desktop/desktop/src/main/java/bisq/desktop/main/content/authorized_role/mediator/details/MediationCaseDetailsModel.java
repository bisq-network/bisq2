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

package bisq.desktop.main.content.authorized_role.mediator.details;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.main.content.authorized_role.mediator.MediationCaseListItem;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class MediationCaseDetailsModel extends NavigationModel {
    private MediationCaseListItem mediationCaseListItem;
    private String tradeDate;
    private String offerType;
    private String market;
    private String fiatAmount;
    private String fiatCurrency;
    private String btcAmount;
    private String price;
    private String priceCodes;
    private String priceSpec;
    private String paymentMethod;
    private String settlementMethod;
    private String tradeId;
    private String buyerUserName;
    private String sellerUserName;
    private String buyerNetworkAddress;
    private String sellerNetworkAddress;

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.MEDIATION_CASE_DETAILS;
    }
}
