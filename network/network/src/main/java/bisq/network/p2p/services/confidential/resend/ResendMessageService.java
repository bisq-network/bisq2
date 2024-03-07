package bisq.network.p2p.services.confidential.resend;

import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ResendMessageService implements PersistenceClient<ResendMessageStore> {
    private final ResendMessageStore persistableStore = new ResendMessageStore();
    private final Persistence<ResendMessageStore> persistence;

    public ResendMessageService(PersistenceService persistenceService,
                                KeyBundleService keyBundleService,
                                NetworkService networkService) {

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    public void initialize() {
    }

    public void shutdown() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void handle(MessageDeliveryStatus messageDeliveryStatus, ResendMessageData resendMessageData) {
        switch (messageDeliveryStatus) {
            case CONNECTING:
                synchronized (this) {
                    persistableStore.getResendMessageDataSet().add(resendMessageData);
                }
                persist();
                break;
            case SENT:
                break;
            case ACK_RECEIVED:
                break;
            case TRY_ADD_TO_MAILBOX:
                break;
            case ADDED_TO_MAILBOX:
                break;
            case MAILBOX_MSG_RECEIVED:
                break;
            case FAILED:
                synchronized (this) {
                    persistableStore.getResendMessageDataSet().remove(resendMessageData);
                }
                persist();
                break;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

}
