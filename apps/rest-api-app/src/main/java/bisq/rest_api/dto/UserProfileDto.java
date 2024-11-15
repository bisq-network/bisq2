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

package bisq.rest_api.dto;

import bisq.common.encoding.Hex;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.security.KeyPair;
import java.util.Base64;

@Getter
@Schema(name = "UserProfileDto")
public final class UserProfileDto {
    private String privateKeyAsBase64;
    private String publicKeyAsBase64;
    private String id;
    private String nym;
    private String powSolutionAsHex;

    public static UserProfileDto from(KeyPair keyPair, String id, String nym, byte[] powSolution) {
        UserProfileDto dto = new UserProfileDto();
        dto.publicKeyAsBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        dto.privateKeyAsBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        dto.id = id;
        dto.nym = nym;
        dto.powSolutionAsHex = Hex.encode(powSolution);
        return dto;
    }
}


