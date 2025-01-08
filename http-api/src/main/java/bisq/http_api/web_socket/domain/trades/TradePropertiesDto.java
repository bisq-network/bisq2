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

package bisq.http_api.web_socket.domain.trades;

import bisq.dto.contract.RoleDto;
import bisq.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

// Collection of all observable fields of BisqEasyTrade. Only the field which was
// updated will be set, the rest stays an empty optional which will result in an
// empty json entry thus not causing overhead.
@Getter
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TradePropertiesDto {
    public Optional<BisqEasyTradeStateDto> tradeState = Optional.empty();
    public Optional<RoleDto> interruptTradeInitiator = Optional.empty();
    public Optional<String> paymentAccountData = Optional.empty();
    public Optional<String> bitcoinPaymentData = Optional.empty();
    public Optional<String> paymentProof = Optional.empty();
    public Optional<String> errorMessage = Optional.empty();
    public Optional<String> errorStackTrace = Optional.empty();
    public Optional<String> peersErrorMessage = Optional.empty();
    public Optional<String> peersErrorStackTrace = Optional.empty();
}