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
    
    private synchronized String getDeviceIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                log.warn("No network interfaces available");
                return "127.0.0.1";
            }
            String preferredIp = null;
            String fallbackIp = null;
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
                    if (addr instanceof java.net.Inet6Address) {
                        continue;
                    }

                    // Prefer private network addresses for local communication
                    if (addr.isSiteLocalAddress() && preferredIp == null) {
                        preferredIp = ip;
                    } else if (fallbackIp == null) {
                        fallbackIp = ip;
                    }
                }
            }
            String result = preferredIp != null ? preferredIp : fallbackIp;
            if (result != null) {
                return result;
            }
        } catch (java.net.SocketException e) {
            log.error("Socket error getting device IP address", e);
        } catch (SecurityException e) {
            log.error("Security error accessing network interfaces", e);
        } catch (Exception e) {
            log.error("Error getting device IP address", e);
        }

        // Fallback to default localhost if we couldn't find a better address
        log.warn("Could not determine device IP, falling back to 127.0.0.1");
        return "127.0.0.1";
    }
}