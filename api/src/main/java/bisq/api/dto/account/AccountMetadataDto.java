package bisq.api.dto.account;

public record AccountMetadataDto(String creationDate,
                                 String tradeLimitInfo,
                                 String tradeDuration) {
}
