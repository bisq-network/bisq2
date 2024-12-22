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

import bisq.dto.security.keys.KeyPairDto;
import bisq.dto.security.pow.ProofOfWorkDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "UserIdentityPreparation")
public final class UserIdentityPreparation {
    @Schema(description = "Key pair",
            example = "{ \"privateKey\": \"MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgky6PNO163DColHrGmSNMgY93amwpAO8ZA8/Pb+Xl5magBwYFK4EEAAqhRANCAARyZim9kPgZixR2+ALUs72fO2zzSkeV89w4oQpkRUct5ob4yHRIIwwrggjoCGmNUWqX/pNA18R46vNYTp8NWuSu\", \"publicKey\": \"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEcmYpvZD4GYsUdvgC1LO9nzts80pHlfPcOKEKZEVHLeaG+Mh0SCMMK4II6AhpjVFql/6TQNfEeOrzWE6fDVrkrg==\" }")
    private KeyPairDto keyPair;
    @Schema(description = "ID", example = "b0edc477ec967379867ae44b1e030fa4f8e68327")
    private String id;
    @Schema(description = "Nym", example = "Ravenously-Poignant-Coordinate-695")
    private String nym;
    @Schema(description = "User statement",
            example = "{ \"payload\": \"[-80, -19, -60, 119, -20, -106, 115, 121, -122, 122, -28, 75, 30, 3, 15, -92, -8, -26, -125, 39]\", \"counter\": 93211, \"difficulty\": 65536.0, \"solution\": [0, 0, 0, 0, 0, 1, 108, 27], \"duration\": 19 }")
    private ProofOfWorkDto proofOfWork;

    public static UserIdentityPreparation from(KeyPairDto keyPair, String id, String nym, ProofOfWorkDto proofOfWork) {
        UserIdentityPreparation dto = new UserIdentityPreparation();
        dto.keyPair = keyPair;
        dto.id = id;
        dto.nym = nym;
        dto.proofOfWork = proofOfWork;
        return dto;
    }
}


