package bisq.dto.security.keys;

public record KeyBundleDto(String keyId, KeyPairDto keyPair, TorKeyPairDto torKeyPair) {
}
