package bisq.network.p2p.services.data.inventory.filter.mini_sketch;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Skeleton for planned MiniSketch implementation based on <a href="https://github.com/sipa/minisketch">https://github.com/sipa/minisketch</a>
 */
@Slf4j
public class MiniSketchFilterService extends FilterService<MiniSketchFilter> {
    public MiniSketchFilterService(StorageService storageService, int maxSize) {
        super(storageService, maxSize);
    }

    public MiniSketchFilter getFilter() {
        return new MiniSketchFilter();
    }

    @Override
    protected boolean isAuthenticatedDataRequestMissing(MiniSketchFilter filter, Map.Entry<ByteArray, AuthenticatedDataRequest> mapEntry) {
        return true;
    }

    @Override
    protected boolean isMailboxRequestMissing(MiniSketchFilter filter, Map.Entry<ByteArray, MailboxRequest> mapEntry) {
        return true;
    }

    @Override
    protected boolean isAddAppendOnlyDataRequestMissing(MiniSketchFilter filter, Map.Entry<ByteArray, AddAppendOnlyDataRequest> mapEntry) {
        return true;
    }

    @Override
    protected MiniSketchFilter safeCast(InventoryFilter inventoryFilter) {
        if (inventoryFilter instanceof MiniSketchFilter &&
                inventoryFilter.getInventoryFilterType() == InventoryFilterType.MINI_SKETCH) {
            return (MiniSketchFilter) inventoryFilter;
        }
        throw new IllegalArgumentException("InventoryFilter not of expected type. inventoryFilter=" + inventoryFilter);
    }
}
