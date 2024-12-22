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

package bisq.http_api.rest_api.domain.user_identity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for creating a new user identity.")
public class CreateUserIdentityRequest {
    @Schema(description = "Nickname for the user", example = "Satoshi", required = true)
    private String nickName;

    @Schema(description = "User terms and conditions", example = "I guarantee to complete the trade in 24 hours")
    private String terms = "";

    @Schema(description = "User statement", example = "I am Satoshi")
    private String statement = "";

    @Schema(description = "Prepared data as JSON object", required = true)
    private UserIdentityPreparation userIdentityPreparation;
}
