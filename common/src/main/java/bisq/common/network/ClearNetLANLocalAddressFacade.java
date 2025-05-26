package bisq.common.network;

import bisq.common.util.NetworkUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.NetworkInterface;
import java.util.Optional;

@Slf4j
public class ClearNetLANLocalAddressFacade implements ClearNetLocalAddressFacade {
    @Setter
    private Optional<NetworkInterface> preferredNetworkInterface;

    public ClearNetLANLocalAddressFacade() {
        this.preferredNetworkInterface = Optional.empty();
    }

    public ClearNetLANLocalAddressFacade(NetworkInterface preferredNetworkInterface) {
        this.preferredNetworkInterface = Optional.of(preferredNetworkInterface);
    }

    /**
     * If preferredNetworkInterface is set and an IPv4 LAN address is found for that network interface we return that.
     * If no preferredNetworkInterface is set, we return the IPv4 LAN address of the first network interface.
     */
    @Override
    public Address toMyLocalAddress(int port) {
        Optional<String> hosts = NetworkUtils.findLANHostAddress(preferredNetworkInterface);
        if (hosts.isEmpty()) {
            log.warn("We did not find any LAN IP address. We use 127.0.0.1 as fallback.");
            return new Address("127.0.0.1", port);
        }
        String host = hosts.get();
        if (preferredNetworkInterface.isPresent()) {
            log.info("My LAN IP address matching my preferred network interface is: {}", host);
        } else {
            log.info("The LAN IP address from my first network interface is: {}", host);
        }
        return new Address(host, port);
    }
}