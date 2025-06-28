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

package bisq.desktop.main.content.user.accounts.details;

import bisq.account.accounts.SepaAccount;
import bisq.account.accounts.SepaAccountPayload;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.locale.CountryRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SepaAccountDetailsVBox extends FiatAccountDetailsVBox<SepaAccount> {
    public SepaAccountDetailsVBox(SepaAccount account) {
        super(account);

    }

    @Override
    protected void addDetails(SepaAccount account) {
        SepaAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValue(Res.get("user.paymentAccounts.sepa.holderName"),
                accountPayload.getHolderName());

        addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.sepa.iban"),
                accountPayload.getIban());

        addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.sepa.bic"),
                accountPayload.getBic());

        String countryName = CountryRepository.getNameByCode(accountPayload.getCountryCode());
        List<String> acceptedCountryCodes = new ArrayList<>(accountPayload.getAcceptedCountryCodes());
        Collections.sort(acceptedCountryCodes);
        List<String> allSepaCountryCodes = new ArrayList<>(FiatPaymentRailUtil.getAllSepaCountryCodes());
        Collections.sort(allSepaCountryCodes);
        boolean matchAllCountries = acceptedCountryCodes.equals(allSepaCountryCodes);
        String allSepaCountries = Res.get("user.paymentAccounts.createAccount.accountData.sepa.allSepaCountries");
        String acceptedCountries = acceptedCountryCodes.stream()
                .map(CountryRepository::getNameByCode)
                .collect(Collectors.joining(", "));
        Label acceptCountriesLabel = addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.createAccount.accountData.sepa.acceptCountries"),
                matchAllCountries ? allSepaCountries : acceptedCountries, acceptedCountries);
        if (matchAllCountries || acceptedCountries.length() > 70) {
            acceptCountriesLabel.setTooltip(new BisqTooltip(acceptedCountries));
        }
    }
}
