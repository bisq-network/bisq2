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

import bisq.account.accounts.F2FAccountPayload;
import bisq.common.locale.CountryRepository;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class F2FDataDisplay extends DataDisplay<F2FAccountPayload> {
    public F2FDataDisplay(F2FAccountPayload accountPayload) {
        super(accountPayload);
    }

    @Override
    protected DataDisplay.Controller<F2FAccountPayload> getController(F2FAccountPayload accountPayload) {
        return new Controller(accountPayload);
    }

    @Slf4j
    public static class Controller extends DataDisplay.Controller<F2FAccountPayload> {
        private Controller(F2FAccountPayload accountPayload) {
            super(accountPayload);
        }

        @Override
        protected DataDisplay.View createView() {
            return new View((Model) model, this);
        }

        @Override
        protected DataDisplay.Model createModel(F2FAccountPayload accountPayload) {
            String countryName = CountryRepository.getNameByCode(accountPayload.getCountryCode());
            return new Model(countryName,
                    accountPayload.getCity(),
                    accountPayload.getContact(),
                    accountPayload.getExtraInfo());
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
        private final String city;
        private final String contact;
        private final String extraInfo;

        protected Model(String country, String city, String contact, String extraInfo) {
            this.country = country;
            this.city = city;
            this.contact = contact;
            this.extraInfo = extraInfo;
        }
    }

    @Slf4j
    protected static class View extends DataDisplay.View {
        protected View(Model model, Controller controller) {
            super(model, controller);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.country"),
                    model.getCountry(),
                    rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.f2f.city"),
                    model.getCity(),
                    ++rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.f2f.contact"),
                    model.getContact(),
                    ++rowIndex);

            addDescriptionAndValue(Res.get("user.paymentAccounts.createAccount.accountData.f2f.extraInfo"),
                    model.getExtraInfo(),
                    ++rowIndex);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
