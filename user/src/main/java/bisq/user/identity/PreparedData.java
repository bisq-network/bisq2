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

package bisq.user.identity;

import bisq.security.keys.KeyPairJsonSer;
import bisq.security.pow.ProofOfWork;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.security.KeyPair;

@Getter
@Schema(name = "PreparedData")
public final class PreparedData {
    @JsonSerialize(using = KeyPairJsonSer.Serializer.class)
    @JsonDeserialize(using = KeyPairJsonSer.Deserializer.class)
    private KeyPair keyPair;
    private String id;
    private String nym;
    private ProofOfWork proofOfWork;

    public static PreparedData from(KeyPair keyPair, String id, String nym, ProofOfWork proofOfWork) {
        PreparedData dto = new PreparedData();
        dto.keyPair = keyPair;
        dto.id = id;
        dto.nym = nym;
        dto.proofOfWork = proofOfWork;
        return dto;
    }
}


