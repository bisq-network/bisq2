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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodChargebackRisk;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.common.util.TextFormatterUtils;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.Parent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PaymentSummaryController implements Controller {
    private final PaymentSummaryModel model;
    @Getter
    private View<? extends Parent, PaymentSummaryModel, ? extends Controller> view;
    private final Runnable createAccountHandler;

    public PaymentSummaryController(Runnable createAccountHandler) {
        this.createAccountHandler = createAccountHandler;
        model = new PaymentSummaryModel();
    }

    @Override
    public void onActivate() {
        updateViewWithAccountData();
    }

    @Override
    public void onDeactivate() {
    }

    public void cleanup() {
        model.setPaymentMethod(null);
        model.setAccountName(null);
        model.setRiskLevel(null);
        model.setTradeLimit(null);
        model.setAccountNameManuallyEdited(false);
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        Optional<PaymentMethod<?>> previousPaymentMethod = model.getPaymentMethod();

        if (previousPaymentMethod.isPresent() && !previousPaymentMethod.equals(Optional.ofNullable(paymentMethod))) {
            model.setAccountNameManuallyEdited(false);
        }

        model.setPaymentMethod(paymentMethod);

        if (view == null || !previousPaymentMethod.equals(Optional.ofNullable(paymentMethod))) {
            view = PaymentSummaryViewFactory.createView(model, this);
        }

        updateViewWithAccountData();
    }

    public void setAccountData(Map<String, Object> accountData) {
        model.setAccountData(accountData != null ? new HashMap<>(accountData) : new HashMap<>());
        updateViewWithAccountData();
    }

    public void createAccount() {
        createAccountHandler.run();
    }

    public void onEditAccountName(String newName) {
        if (StringUtils.isNotEmpty(newName)) {
            model.setAccountName(newName.trim());
            model.setAccountNameManuallyEdited(true);
            if (view instanceof PaymentSummaryView) {
                ((PaymentSummaryView) view).updateAccountName(newName);
            }
        }
    }

    public Optional<String> getAccountName() {
        return model.getAccountName();
    }

    private void updateViewWithAccountData() {
        Map<String, Object> accountData = model.getAccountData();
        if (accountData.isEmpty()) {
            return;
        }

        Optional<PaymentMethod<?>> paymentMethodOpt = model.getPaymentMethod();
        if (paymentMethodOpt.isEmpty()) {
            return;
        }

        PaymentMethod<?> paymentMethod = paymentMethodOpt.get();

        model.clearTooltipData();
        if (!model.isAccountNameManuallyEdited()) {
            model.setAccountName(generateAccountName(paymentMethod, accountData));
        }

        if (paymentMethod instanceof FiatPaymentMethod fiatMethod) {
            switch (fiatMethod.getPaymentRail()) {
                case F2F -> configureF2FSummary(accountData);
                case SEPA -> configureSepaAccountSummary(accountData);
            }
        }

        model.setRiskLevel(getRiskLevel(paymentMethod));

        Country country = getCountryOrDefault(model.getAccountData());
        model.setTradeLimit(FiatPaymentRailUtil.getTradeLimit(paymentMethod, country));
    }

    private void configureF2FSummary(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        Country country = (Country) data.get("country");
        String city = StringUtils.toOptional((String) data.get("city")).orElse("");
        String contact = StringUtils.toOptional((String) data.get("contact")).orElse("");
        String extraInfo = StringUtils.toOptional((String) data.get("extraInfo")).orElse("");

        Map<String, String> summaryMap = new LinkedHashMap<>();
        if (country != null) {
            summaryMap.put(Res.get("user.paymentAccounts.summary.country"), country.getName());
        }
        summaryMap.put(Res.get("user.paymentAccounts.summary.city"), city);
        summaryMap.put(Res.get("user.paymentAccounts.summary.contact"), contact);
        model.addFullTextForTooltip("contact", contact);

        summaryMap.put(Res.get("user.paymentAccounts.summary.extraInfo"), extraInfo);
        model.addFullTextForTooltip("extraInfo", extraInfo);
        model.setSummaryDetails(summaryMap);
    }

    private void configureSepaAccountSummary(Map<String, Object> data) {
        Country country = getCountryOrDefault(data);
        String holderName = StringUtils.toOptional((String) data.get("holderName")).orElse("");
        String iban = StringUtils.toOptional((String) data.get("iban")).orElse("").toUpperCase();
        String bic = StringUtils.toOptional((String) data.get("bic")).orElse("").toUpperCase();

        @SuppressWarnings("unchecked")
        List<String> acceptedCountryCodes = (List<String>) data.get("acceptedCountryCodes");

        Map<String, String> summaryMap = new LinkedHashMap<>();
        if (country != null) {
            summaryMap.put(Res.get("user.paymentAccounts.summary.country"), country.getName());
        }
        summaryMap.put(Res.get("user.paymentAccounts.summary.currency"), "EUR");

        summaryMap.put(Res.get("user.paymentAccounts.summary.holderName"), holderName);
        model.addFullTextForTooltip("holderName", holderName);

        String formattedIban = TextFormatterUtils.formatIban(iban);
        summaryMap.put(Res.get("user.paymentAccounts.summary.iban"), formattedIban);
        model.addFullTextForTooltip("iban", iban);

        summaryMap.put(Res.get("user.paymentAccounts.summary.bic"), bic);
        if (acceptedCountryCodes != null && !acceptedCountryCodes.isEmpty()) {
            String completeCountriesList = String.join(", ", acceptedCountryCodes);
            summaryMap.put(Res.get("user.paymentAccounts.summary.acceptedCountries"), completeCountriesList);
            model.addFullTextForTooltip("acceptedCountries", completeCountriesList);
        }
        model.setSummaryDetails(summaryMap);
    }

    private Country getCountryOrDefault(Map<String, Object> data) {
        return Optional.ofNullable((Country) data.get("country"))
                .orElse(CountryRepository.getDefaultCountry());
    }

    private String generateAccountName(PaymentMethod<?> paymentMethod, Map<String, Object> data) {
        String baseName = paymentMethod.getShortDisplayString();
        String suffix = "";

        if (paymentMethod instanceof FiatPaymentMethod fiatMethod) {
            switch (fiatMethod.getPaymentRail()) {
                case SEPA -> {
                    // Use the last 4 digits of IBAN for quick identification
                    String iban = StringUtils.toOptional((String) data.get("iban")).orElse("");
                    if (iban.length() > 4) {
                        suffix = " - " + iban.substring(iban.length() - 4);
                    }
                }
                case F2F -> {
                    // Use city and country for geographical identification
                    String city = StringUtils.toOptional((String) data.get("city")).orElse("");
                    Country country = (Country) data.get("country");

                    if (city.length() > 15) {
                        city = city.substring(0, 15);
                    }

                    if (!city.isEmpty() && country != null) {
                        suffix = " " + city + ", " + country.getCode();
                    } else if (!city.isEmpty()) {
                        suffix = " " + city;
                    } else if (country != null) {
                        suffix = " " + country.getName();
                    }
                }
            }
        }
        return baseName + suffix;
    }

    private String getRiskLevel(PaymentMethod<?> paymentMethod) {
        if (paymentMethod instanceof FiatPaymentMethod fiatMethod) {
            FiatPaymentMethodChargebackRisk riskEnum = fiatMethod.getPaymentRail().getChargebackRisk();

            return switch (riskEnum) {
                case LOW -> Res.get("user.paymentAccounts.summary.risk.low");
                case MEDIUM -> Res.get("user.paymentAccounts.summary.risk.medium");
                case HIGH -> Res.get("user.paymentAccounts.summary.risk.high");
            };
        }

        return Res.get("user.paymentAccounts.summary.risk.unknown");
    }
}