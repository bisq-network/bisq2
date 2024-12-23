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

package bisq.dto.offer.payment_method;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString
@Getter
@EqualsAndHashCode

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, 
        include = JsonTypeInfo.As.PROPERTY, 
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FiatPaymentMethodSpecDto.class, name = "FiatPaymentMethodSpec"),
        @JsonSubTypes.Type(value = BitcoinPaymentMethodSpecDto.class, name = "BitcoinPaymentMethodSpec"),
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class PaymentMethodSpecDto {
    protected final Optional<String> saltedMakerAccountId;
    protected final String paymentMethod;

    protected PaymentMethodSpecDto(String paymentMethod) {
        this(paymentMethod, Optional.empty());
    }

    protected PaymentMethodSpecDto(String paymentMethod, Optional<String> saltedMakerAccountId) {
        this.paymentMethod = paymentMethod;
        this.saltedMakerAccountId = saltedMakerAccountId;
    }
}
