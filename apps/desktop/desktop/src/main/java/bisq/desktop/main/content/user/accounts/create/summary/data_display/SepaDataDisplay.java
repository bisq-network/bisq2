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

package bisq.desktop.main.content.user.accounts.create.summary.data_display;

import bisq.account.accounts.SepaAccountPayload;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.locale.CountryRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.scene.control.Label;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SepaDataDisplay extends DataDisplay<SepaAccountPayload> {
    public SepaDataDisplay(SepaAccountPayload accountPayload) {
        super(accountPayload);
    }

    @Override
    protected DataDisplay.Controller<SepaAccountPayload> getController(SepaAccountPayload accountPayload) {
        return new Controller(accountPayload);
    }

    @Slf4j
    public static class Controller extends DataDisplay.Controller<SepaAccountPayload> {
        private Controller(SepaAccountPayload accountPayload) {
            super(accountPayload);
        }

        @Override
        protected DataDisplay.View createView() {
            return new View((Model) model, this);
        }

        @Override
        protected DataDisplay.Model createModel(SepaAccountPayload accountPayload) {
            String countryName = CountryRepository.getNameByCode(accountPayload.getCountryCode());
            List<String> acceptedCountryCodes = new ArrayList<>(accountPayload.getAcceptedCountryCodes());
            Collections.sort(acceptedCountryCodes);
            List<String> allSepaCountries = new ArrayList<>(FiatPaymentRailUtil.getAllSepaCountries());
            Collections.sort(allSepaCountries);
            String acceptCountries;
            if (acceptedCountryCodes.equals(allSepaCountries)) {
                acceptCountries = Res.get("user.paymentAccounts.createAccount.accountData.sepa.allSepaCountries");
            } else {
                acceptCountries = acceptedCountryCodes.stream()
                        .map(CountryRepository::getNameByCode)
                        .collect(Collectors.joining(", "));
            }

            return new Model(countryName,
                    accountPayload.getHolderName(),
                    accountPayload.getIban(),
                    accountPayload.getBic(),
                    acceptCountries);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    protected static class Model extends DataDisplay.Model {
        private final String country;
        private final String holderName;
        private final String iban;
        private final String bic;
        private final String acceptCountries;

        protected Model(String country,
                        String holderName,
                        String iban,
                        String bic,
                        String acceptCountries) {
            this.country = country;
            this.holderName = holderName;
            this.iban = iban;
            this.bic = bic;
            this.acceptCountries = acceptCountries;
        }
    }

    @Slf4j
    protected static class View extends DataDisplay.View {

        protected View(Model model, Controller controller) {
            super(model, controller);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.country"),
                    model.getCountry(),
                    rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.sepa.holderName"),
                    model.getHolderName(),
                    ++rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.sepa.iban"),
                    model.getIban(),
                    ++rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.sepa.bic"),
                    model.getBic(),
                    ++rowIndex);

            String acceptCountries = model.getAcceptCountries();
            Label acceptCountriesLabel = addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.sepa.acceptCountries"),
                    acceptCountries,
                    ++rowIndex);
            if (acceptCountries.length() > 70) {
                acceptCountriesLabel.setTooltip(new BisqTooltip(acceptCountries));
            }
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
