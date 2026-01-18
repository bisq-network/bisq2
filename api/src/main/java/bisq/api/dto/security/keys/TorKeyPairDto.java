package bisq.api.dto.security.keys;

public record TorKeyPairDto (String privateKeyEncoded, String publicKeyEncoded, String onionAddress) {
}
