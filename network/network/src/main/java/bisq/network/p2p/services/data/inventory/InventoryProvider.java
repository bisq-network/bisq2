package bisq.network.p2p.services.data.inventory;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;

import java.util.Map;
import java.util.stream.Stream;

public interface InventoryProvider {
    Stream<Map<ByteArray, AuthenticatedDataRequest>> getAuthenticatedDataStoreMaps();

    Stream<Map<ByteArray, MailboxRequest>> getMailboxStoreMaps();

    Stream<Map<ByteArray, AddAppendOnlyDataRequest>> getAddAppendOnlyDataStoreMaps();

    FilterEntry getFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry);
}
