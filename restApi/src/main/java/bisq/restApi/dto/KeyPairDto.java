package bisq.restApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(title = "KeyPair")
public class KeyPairDto {
    byte[] publicKey;
    byte[] privateKey;

    public KeyPairDto(java.security.KeyPair k) {
        if (k != null) {
            publicKey = k.getPublic().getEncoded();
            privateKey = k.getPrivate().getEncoded();
        }
    }
}


