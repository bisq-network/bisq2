package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.util.BankAccountUtils;
import bisq.api.dto.account.fiat.common.BankAccountCountryDetailsDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;

import java.util.Comparator;
import java.util.List;

public final class BankAccountCountryDetailsDtoMapping {
    private static final List<BankAccountCountryDetailsDto> BANK_ACCOUNT_COUNTRY_DETAILS = CountryRepository.getAllCountries().stream()
            .map(BankAccountCountryDetailsDtoMapping::fromCountry)
            .sorted(Comparator.comparing(details -> details.country().name(), String.CASE_INSENSITIVE_ORDER))
            .toList();

    private BankAccountCountryDetailsDtoMapping() {
    }

    public static List<BankAccountCountryDetailsDto> getAll() {
        return BANK_ACCOUNT_COUNTRY_DETAILS;
    }

    private static BankAccountCountryDetailsDto fromCountry(Country country) {
        String countryCode = country.getCode();
        return new BankAccountCountryDetailsDto(
                new CountryDto(
                        countryCode,
                        CountryRepository.getLocalizedCountryDisplayString(countryCode)
                ),
                BankAccountUtils.useValidation(countryCode),
                BankAccountUtils.isHolderIdRequired(countryCode),
                BankAccountUtils.getHolderIdDescription(countryCode),
                BankAccountUtils.getHolderIdDescriptionShort(countryCode),
                BankAccountUtils.isBankAccountTypeRequired(countryCode),
                BankAccountUtils.isBankNameRequired(countryCode),
                BankAccountUtils.isBankIdRequired(countryCode),
                BankAccountUtils.getBankIdDescription(countryCode),
                BankAccountUtils.getBankIdDescriptionShort(countryCode),
                BankAccountUtils.isBranchIdRequired(countryCode),
                BankAccountUtils.getBranchIdDescription(countryCode),
                BankAccountUtils.getBranchIdDescriptionShort(countryCode),
                BankAccountUtils.getAccountNrDescription(countryCode),
                BankAccountUtils.isNationalAccountIdRequired(countryCode),
                BankAccountUtils.getNationalAccountIdDescription(countryCode),
                BankAccountUtils.getNationalAccountIdDescriptionShort(countryCode)
        );
    }
}
