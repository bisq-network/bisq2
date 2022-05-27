package bisq.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(title = "KeyPair")
public class KeyPairDTO {
    byte[] publicKey;
    byte[] privateKey;

    public KeyPairDTO(java.security.KeyPair k) {
        if (k != null) {
            publicKey = k.getPublic().getEncoded();
            privateKey = k.getPrivate().getEncoded();
        }
    }
}


