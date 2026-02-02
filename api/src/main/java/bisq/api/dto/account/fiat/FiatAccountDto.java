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

package bisq.api.dto.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentRail;
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
    @JsonSubTypes.Type(value = UserDefinedFiatAccountDto.class, name = "CUSTOM")
    // TODO: Add more when implemented:
    // @JsonSubTypes.Type(value = SepaAccountDto.class, name = "SEPA"),
    // @JsonSubTypes.Type(value = RevolutAccountDto.class, name = "REVOLUT"),
    // @JsonSubTypes.Type(value = ZelleAccountDto.class, name = "ZELLE"),
})
public sealed interface FiatAccountDto 
        permits UserDefinedFiatAccountDto {
    // TODO: Add more permitted types when implemented:
    // permits UserDefinedFiatAccountDto, SepaAccountDto, RevolutAccountDto, ZelleAccountDto, etc.
    
    String accountName();
    FiatPaymentRail paymentRail();
    FiatAccountPayloadDto accountPayload();
}

