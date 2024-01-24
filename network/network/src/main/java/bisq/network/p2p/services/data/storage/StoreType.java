package bisq.network.p2p.services.data.storage;

import lombok.Getter;

public enum StoreType {
    ALL(""),
    AUTHENTICATED_DATA_STORE("authenticated"),
    MAILBOX_DATA_STORE("mailbox"),
    APPEND_ONLY_DATA_STORE("append");
    @Getter
    private final String storeName;

    StoreType(String storeName) {
        this.storeName = storeName;
    }
}
