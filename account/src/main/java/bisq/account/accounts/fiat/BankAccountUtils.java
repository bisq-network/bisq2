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

package bisq.account.accounts.fiat;

import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;

import java.util.HashSet;
import java.util.Set;

public class BankAccountUtils {
    public static boolean isBankAccountTypeRequired(String countryCode) {
        return switch (countryCode) {
            case "US", "BR", "CA", "IN" -> true;
            default -> false;
        };
    }

    public static boolean isHolderIdRequired(String countryCode) {
        return switch (countryCode) {
            case "BR", "CL", "AR", "IN", "SA", "ID" -> true;
            default -> false;
        };
    }

    public static String getHolderIdDescription(String countryCode) {
        return switch (countryCode) {
            case "BR" -> "Cadastro de Pessoas Físicas (CPF)"; // do not translate as it is used in Portuguese only
            case "CL" -> "Rol Único Tributario (RUT)";  // do not translate as it is used in Spanish only
            case "AR" -> "CUIL/CUIT";
            case "IN" -> "PAN or Aadhaar";
            case "SA" -> "South African ID number";
            case "ID" -> "Nomor KTP Indonesia";     // Indonesia KTP number
            default -> Res.get("paymentAccounts.bank.holderId");
        };
    }

    public static String getHolderIdDescriptionShort(String countryCode) {
        return switch (countryCode) {
            case "BR" -> "CPF";
            case "CL" -> "RUT";
            case "AR" -> "CUIT";
            case "IN" -> "PAN";
            case "SA" -> "SA ID";
            case "ID" -> "KTP";
            default -> "ID";
        };
    }

    public static boolean isBankNameRequired(String countryCode) {
        Set<String> notRequired = new HashSet<>(FiatPaymentRailUtil.getSepaNonEuroCountries());
        return !notRequired.contains(countryCode);
    }

    public static boolean isBankIdRequired(String countryCode) {
        return switch (countryCode) {
            case "GB", "NZ", "AU", "SE", "CL", "NO", "IN", "JP" -> false;
            default -> true;
        };
    }

    public static String getBankIdDescription(String countryCode) {
        return switch (countryCode) {
            case "CA" -> "Institution Number"; // do not translate as it is used in English only
            case "MX", "HK" -> Res.get("paymentAccounts.bank.bankCode");
            case "US" -> "Routing Number"; // do not translate as it is used in English only
            case "IN" -> "IFSC Code";
            default -> isBankIdRequired(countryCode) ?
                    Res.get("paymentAccounts.bank.bankId") :
                    Res.get("paymentAccounts.bank.bankIdOptional");
        };
    }
    public static String getBankIdDescriptionShort(String countryCode) {
        return switch (countryCode) {
            case "CA" -> "Institution No."; // Shorter informal synonym for Institution Number (often used in online forms)
            case "MX", "HK" -> Res.get("paymentAccounts.bank.bankCode");
            case "US" -> "Routing No."; // do not translate as it is used in English only
            case "IN" -> "IFSC";
            default -> Res.get("paymentAccounts.bank.bankIdShort");
        };
    }

    public static boolean isBranchIdRequired(String countryCode) {
        Set<String> notRequired = new HashSet<>(FiatPaymentRailUtil.getSepaNonEuroCountries());
        notRequired.remove("GB");
        notRequired.addAll(Set.of("NZ", "AU", "MX", "HK", "SE", "NO", "US", "IN", "JP"));
        return !notRequired.contains(countryCode);
    }

    public static String getBranchIdDescription(String countryCode) {
        return switch (countryCode) {
            case "GB" -> "UK sort code"; // do not translate as it is used in English only
            case "BR" -> "Código da Agência"; // do not translate as it is used in Portuguese only
            case "AU" -> "BSB code"; // do not translate as it is used in English only
            case "CA" -> "Transit Number"; // do not translate as it is used in English only
            default -> isBranchIdRequired(countryCode) ?
                    Res.get("paymentAccounts.bank.branchId") :
                    Res.get("paymentAccounts.bank.branchIdOptional");
        };
    }
    public static String getBranchIdDescriptionShort(String countryCode) {
        return switch (countryCode) {
            case "GB" -> "Sort code"; // do not translate as it is used in English only
            case "BR" -> "Agência"; // do not translate as it is used in Portuguese only
            case "AU" -> "BSB"; // do not translate as it is used in English only
            case "CA" -> "Transit No."; // do not translate as it is used in English only
            default -> isBranchIdRequired(countryCode) ?
                    Res.get("paymentAccounts.bank.branchId") :
                    Res.get("paymentAccounts.bank.branchIdOptional");
        };
    }

    public static String getAccountNrDescription(String countryCode) {
        return switch (countryCode) {
            case "GB", "US", "BR", "NZ", "AU", "CA", "HK" -> Res.get("paymentAccounts.accountNr");
            case "NO", "SE" -> "Kontonummer"; // do not translate as it is used in Norwegian and Swedish only
            case "MX" -> "CLABE"; // do not translate as it is used in Spanish only
            case "CL" -> "Cuenta"; // do not translate as it is used in Spanish only
            case "AR" -> "Número de cuenta"; // do not translate as it is used in Spanish only
            default -> Res.get("paymentAccounts.bank.accountNrOrIban");
        };
    }

    public static boolean isNationalAccountIdRequired(String countryCode) {
        //noinspection SwitchStatementWithTooFewBranches
        return switch (countryCode) {
            case "AR", "IN" -> true;
            default -> false;
        };
    }

    public static String getNationalAccountIdDescription(String countryCode) {
        //noinspection SwitchStatementWithTooFewBranches
        return switch (countryCode) {
            //CBU (Clave Bancaria Uniforme) is used for identifying accounts in Argentina; CVU (used by fintechs like MercadoPago)
            case "AR" -> "CBU o CVU";
            case "IN" -> "IFSC";
            default -> "";
        };
    }
    public static String getNationalAccountIdDescriptionShort(String countryCode) {
        //noinspection SwitchStatementWithTooFewBranches
        return switch (countryCode) {
            //CBU (Clave Bancaria Uniforme) is used for identifying accounts in Argentina; CVU (used by fintechs like MercadoPago)
            case "AR" -> "CBU";
            case "IN" -> "IFSC";
            default -> "";
        };
    }

    public static boolean useValidation(String countryCode) {
        return switch (countryCode) {
            case "GB", "US", "BR", "AU", "CA", "NZ", "MX", "HK", "SE", "NO", "AR" -> true;
            default -> false;
        };
    }

    // If we use custom descriptions using the pattern "Enter {StringUtils.unCapitalize(description)})" does not work well
    public static String getPrompt(String countryCode, String description) {
        return switch (countryCode) {
            case "AR", "IN", "NO", "SE", "MX", "CL", "GB", "BR", "AU", "CA", "US", "SA" -> description;
            default -> Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(description));
        };
    }
}
