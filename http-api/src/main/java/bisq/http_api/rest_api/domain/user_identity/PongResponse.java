package bisq.http_api.rest_api.domain.user_identity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
@Getter
@Schema(name = "Pong")
public class PongResponse {
    @Schema(description = "message")
    private final String message = "pong";
}
