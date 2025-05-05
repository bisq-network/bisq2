package bisq.dto.security.keys;

public record I2pKeyPairDto(String privateKeyEncoded, String publicKeyEncoded, String destination) {
}
