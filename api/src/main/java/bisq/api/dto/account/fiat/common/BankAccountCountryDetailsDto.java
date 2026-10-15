package bisq.api.dto.account.fiat.common;

public record BankAccountCountryDetailsDto(
        CountryDto country,
        boolean bankAccountValidationSupported,
        boolean holderIdRequired,
        String holderIdDescription,
        String holderIdDescriptionShort,
        boolean bankAccountTypeRequired,
        boolean bankNameRequired,
        boolean bankIdRequired,
        String bankIdDescription,
        String bankIdDescriptionShort,
        boolean branchIdRequired,
        String branchIdDescription,
        String branchIdDescriptionShort,
        String accountNrDescription,
        boolean nationalAccountIdRequired,
        String nationalAccountIdDescription,
        String nationalAccountIdDescriptionShort
) {
}
