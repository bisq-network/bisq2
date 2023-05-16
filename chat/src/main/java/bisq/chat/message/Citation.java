package bisq.chat.message;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Citation implements Proto {
    private final String authorUserProfileId;
    private final String text;

    public Citation(String authorUserProfileId, String text) {
        this.authorUserProfileId = authorUserProfileId;
        this.text = text;
    }

    public bisq.chat.protobuf.Citation toProto() {
        return bisq.chat.protobuf.Citation.newBuilder()
                .setAuthorUserProfileId(authorUserProfileId)
                .setText(text)
                .build();
    }

    public static Citation fromProto(bisq.chat.protobuf.Citation proto) {
        return new Citation(proto.getAuthorUserProfileId(),
                proto.getText());
    }

    public boolean isValid() {
        return authorUserProfileId != null && !authorUserProfileId.isEmpty() &&
                text != null && !text.isEmpty();
    }
}
