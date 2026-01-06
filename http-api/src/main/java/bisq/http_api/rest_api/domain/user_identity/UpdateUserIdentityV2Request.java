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
@Schema(description = "Request payload for updating user profile statement and trade terms.")
public class UpdateUserIdentityV2Request {

    @Schema(description = "The profile id we are trying to update")
    private String profileId;

    @Schema(description = "The updated statement for the user profile.", example = "I am a reliable trader.")
    private String statement;

    @Schema(description = "The updated trade terms for the user profile.", example = "No trades on weekends.")
    private String terms;
}