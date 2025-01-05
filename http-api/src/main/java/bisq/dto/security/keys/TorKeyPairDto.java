package bisq.dto.security.keys;

public record TorKeyPairDto (String privateKey, String publicKey, String onionAddress) {
}
