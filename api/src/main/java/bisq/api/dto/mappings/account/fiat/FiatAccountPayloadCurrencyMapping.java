package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.common.asset.FiatCurrencyRepository;

public class FiatAccountPayloadCurrencyMapping {
    public static String toDisplayString(AccountPayload<?> accountPayload) {
        return switch (accountPayload) {
            case MultiCurrencyAccountPayload multiCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayNames(multiCurrencyAccountPayload.getSelectedCurrencyCodes());
            case SelectableCurrencyAccountPayload selectableCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(selectableCurrencyAccountPayload.getSelectedCurrencyCode());
            case SingleCurrencyAccountPayload singleCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(singleCurrencyAccountPayload.getCurrencyCode());
            case null, default -> {
                String type = accountPayload != null ? accountPayload.getClass().getSimpleName() : "null";
                throw new UnsupportedOperationException("accountPayload of unexpected type: " + type);
            }
        };
    }
}
