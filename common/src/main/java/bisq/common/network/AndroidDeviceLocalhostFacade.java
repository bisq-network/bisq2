package bisq.common.network;

import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Slf4j
public class AndroidDeviceLocalhostFacade implements LocalhostFacade {
    
    public Address toMyLocalhost(int port) {
        String deviceIp = getDeviceIpAddress();
        log.info("Android device using IP address: {}", deviceIp);
        return new Address(deviceIp, port);
    }

    public Address toPeersLocalhost(Address address) {
        // For real devices, we don't need to modify peer addresses
        return address;
    }
    
    private String getDeviceIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    
                    // Skip IPv6 addresses
                    if (ip.contains(":")) {
                        continue;
                    }
                    
                    // Found a valid IPv4 address
                    log.debug("Found device IP: {} on interface: {}", ip, iface.getDisplayName());
                    return ip;
                }
            }
        } catch (Exception e) {
            log.error("Error getting device IP address", e);
        }
        
        // Fallback to default localhost if we couldn't find a better address
        log.warn("Could not determine device IP, falling back to 127.0.0.1");
        return "127.0.0.1";
    }
}