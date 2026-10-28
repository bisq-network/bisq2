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

package bisq.support.notifications;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.observable.Observable;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.notifications.Notification;
import bisq.support.moderator.ReportToModeratorMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString
@Getter
@EqualsAndHashCode
public class SupportNotification implements PersistableProto, NetworkProto, Notification {
    private final String id;
    private final String title;
    private final SupportNotificationType type;
    private final Optional<String> channelId;
    private final Optional<String> senderUserProfileId;
    private final String message;
    @EqualsAndHashCode.Exclude
    private final Observable<Boolean> isConsumed = new Observable<>();

    public SupportNotification(ReportToModeratorMessage message) {
        this.id = createId(message);
        this.type = SupportNotificationType.MODERATOR_NOTIFICATION;
        this.channelId = Optional.empty();
        this.senderUserProfileId = Optional.empty();
        this.title = "User Report";
        this.message = message.getMessage();
        this.setConsumed(false);
    }

    public SupportNotification(BisqEasyOpenTradeMessage message) {
        this.id = message.getId();
        this.type = SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION;
        this.channelId = Optional.of(message.getChannelId());
        this.senderUserProfileId = Optional.of(message.getAuthorUserProfileId());
        this.title = "Bisq Easy Trade Message";
        this.message = message.getText().orElse("");
        this.setConsumed(false);
    }

    public SupportNotification(MuSigOpenTradeMessage message) {
        this.id = message.getId();
        this.type = SupportNotificationType.MU_SIG_MEDIATOR_NOTIFICATION;
        this.channelId = Optional.of(message.getChannelId());
        this.senderUserProfileId = Optional.of(message.getAuthorUserProfileId());
        this.title = "Mu-Sig Trade Message";
        this.message = message.getText().orElse("");
        this.setConsumed(false);
    }

    public SupportNotification(
            String id,
            bisq.support.protobuf.SupportNotificationType supportNotificationType,
            Optional<String> channelId,
            Optional<String> senderUserProfileId,
            String title,
            String message,
            boolean isConsumed
    ) {
        this.type = SupportNotificationType.fromProtoEnum(supportNotificationType);
        this.id = id;
        this.channelId = channelId;
        this.senderUserProfileId = senderUserProfileId;
        this.title = title;
        this.message = message;
        this.isConsumed.set(isConsumed);
    }

    public static SupportNotification fromProto(bisq.support.protobuf.SupportNotification proto) {
        return new SupportNotification(
                proto.getId(),
                proto.getSupportNotificationType(),
                proto.hasChannelId() ? Optional.of(proto.getChannelId()) : Optional.empty(),
                proto.hasSenderUserProfileId() ? Optional.of(proto.getSenderUserProfileId()) : Optional.empty(),
                proto.getTitle(),
                proto.getMessage(),
                proto.getIsConsumed()
        );
    }

    public bisq.support.protobuf.SupportNotification toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.support.protobuf.SupportNotification.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.SupportNotification.Builder builder = bisq.support.protobuf.SupportNotification.newBuilder()
                .setId(id)
                .setTitle(title)
                .setMessage(message)
                .setSupportNotificationType(type.toProtoEnum())
                .setIsConsumed(isConsumed.get());
        getChannelId().ifPresent(builder::setChannelId);
        getSenderUserProfileId().ifPresent(builder::setSenderUserProfileId);
        return builder;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(channelId);
    }

    public static String createId(ReportToModeratorMessage message) {
        return message.getDate() + "." + message.getReporterUserProfileId();
    }

    public void setConsumed(boolean consumed) {
        this.isConsumed.set(consumed);
    }

    public boolean isNotConsumed() {
        return isConsumed.get() == false;
    }

    public boolean isMediatorNotification(){
        return type == SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION
                || type == SupportNotificationType.MU_SIG_MEDIATOR_NOTIFICATION;
    }

    public boolean isBisqEasyMediatorNotification(){
        return type == SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION;
    }

    public boolean isMuSigMediatorNotification(){
        return type == SupportNotificationType.MU_SIG_MEDIATOR_NOTIFICATION;
    }

    public boolean isModeratorNotification(){
        return type == SupportNotificationType.MODERATOR_NOTIFICATION;
    }
}
