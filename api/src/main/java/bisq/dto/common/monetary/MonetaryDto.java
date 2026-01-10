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

package bisq.dto.common.monetary;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CoinDto.class, name = "Coin"),
        @JsonSubTypes.Type(value = FiatDto.class, name = "Fiat"),
})
@EqualsAndHashCode
@Getter
public abstract class MonetaryDto {
    protected final String id;
    protected final long value;
    protected final String code;
    protected final int precision;
    protected final int lowPrecision;

    protected MonetaryDto(String id, long value, String code, int precision, int lowPrecision) {
        this.id = id;
        this.value = value;
        this.code = code;
        this.precision = precision;
        this.lowPrecision = lowPrecision;
    }
}
