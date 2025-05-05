package bisq.network.i2p.util;

import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.router.RouterContext;
import net.i2p.client.naming.SingleFileNamingService;
import net.i2p.client.naming.NamingService;
import net.i2p.client.naming.HostsTxtNamingService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class I2PNameResolver {

    private final RouterContext ctx;
    private final NamingService namingService;
    private final ConcurrentHashMap<String, Destination> cache = new ConcurrentHashMap<>();

    public I2PNameResolver(RouterContext context) {
        this.ctx = context;
        this.namingService = SingleFileNamingService.createInstance(context);
    }

    public Destination resolve(String hostname) {
        // Check cache first
        Destination cachedDest = cache.get(hostname);
        if (cachedDest != null) {
            log.debug("Cache hit for hostname: {}", hostname);
            return cachedDest;
        }

        // Attempt resolution
        try {
            // First try Blockfile/NetDb
            Destination dest = namingService.lookup(hostname);
            if (dest != null) {
                log.debug("Resolved via NetDB: {}", hostname);
                cache.put(hostname, dest); // Cache the result
                return dest;
            }

            // Fall back to host.txt
            HostsTxtNamingService hostFile = new HostsTxtNamingService(ctx);
            dest = hostFile.lookup(hostname);
            if (dest != null) {
                log.debug("Resolved via host.txt: {}", hostname);
                cache.put(hostname, dest); // Cache the result
                return dest;
            }

            // Check if it's a valid base32 (can reconstruct Destination from it)
            if (hostname.endsWith(".b32.i2p")) {
                String base32 = hostname.substring(0, hostname.indexOf(".b32.i2p"));
                byte[] hash = Base32.decode(base32);
                dest = ctx.netDb().lookupDestinationLocally(new Hash(hash));
                if (dest != null) {
                    log.debug("Reconstructed Destination from base32: {}", hostname);
                    cache.put(hostname, dest); // Cache the result
                    return dest;
                }
            }

        } catch (Exception e) {
            log.warn("Failed to resolve hostname: {}", hostname, e);
        }

        // If no resolution found, return null and optionally retry after a delay
        return null;
    }
}
