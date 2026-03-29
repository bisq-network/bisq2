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

package bisq.api.dto.account;

import bisq.api.dto.account.crypto.MoneroAccountDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountDto;
import bisq.api.dto.account.fiat.ZelleAccountDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all fiat payment account DTOs.
 * Each fiat payment rail type (CUSTOM, SEPA, REVOLUT, etc.) will have its own implementation.
 * All fiat account DTOs must have an account name, payment rail type, and payload.
 * 
 * Jackson uses the paymentRail field to determine which concrete type to deserialize to.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "paymentRail",
    visible = true
)
@JsonSubTypes({
        // Fiat
        @JsonSubTypes.Type(value = UserDefinedFiatAccountDto.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = ZelleAccountDto.class, name = "ZELLE"),
        // Crypto
        @JsonSubTypes.Type(value = MoneroAccountDto.class, name = "MONERO"),
        @JsonSubTypes.Type(value = OtherCryptoAssetAccountDto.class, name = "OTHER_CRYPTO_ASSET")
})
public interface PaymentAccountDto {
    String accountName();

    PaymentRailDto paymentRail();

    PaymentAccountPayloadDto accountPayload();

    Long creationDate();
}

