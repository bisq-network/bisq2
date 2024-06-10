package bisq.tor.controller.events.listener;

import bisq.tor.controller.events.events.HsDescEvent;

public interface HsDescEventListener {
    void onHsDescEvent(HsDescEvent hsDescEvent);
}
