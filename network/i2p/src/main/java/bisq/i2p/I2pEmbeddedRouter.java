package bisq.i2p;

import bisq.common.threading.ThreadName;
import bisq.common.timer.Scheduler;
import bisq.i2p.util.I2PLogManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class I2pEmbeddedRouter {

    //TODO - Restart of embedded router not implemented yet

    private Router router;
    private RouterContext routerContext;
    protected CommSystemFacade.Status i2pRouterStatus;
    private final int inboundKBytesPerSecond;
    private final int outboundKBytesPerSecond;
    private final int bandwidthSharePercentage;
    private final String dirPath;
    private static boolean initialized = false;

    private static I2pEmbeddedRouter localRouter;

    public static I2pEmbeddedRouter getI2pEmbeddedRouter(String i2pDirPath, int inboundKBytesPerSecond, int outboundKBytesPerSecond,
                                                         int bandwidthSharePercentage, boolean extendedI2pLogging) {
        if(!initialized) {
            initialized = true;
        }
        if(null == localRouter) {
            localRouter = new I2pEmbeddedRouter(i2pDirPath,
                    inboundKBytesPerSecond,
                    outboundKBytesPerSecond,
                    bandwidthSharePercentage,
                    extendedI2pLogging);
        }
        return localRouter;
    }

    public static boolean isInitialized(){
        return initialized;
    }

    public static I2pEmbeddedRouter getInitializedI2pEmbeddedRouter() {
        return localRouter;
    }

    private I2pEmbeddedRouter(String i2pDirPath, int inboundKBytesPerSecond, int outboundKBytesPerSecond,
                              int bandwidthSharePercentage, boolean extendedI2pLogging) {
        this.dirPath = i2pDirPath;
        this.inboundKBytesPerSecond = inboundKBytesPerSecond;
        this.outboundKBytesPerSecond = outboundKBytesPerSecond;
        this.bandwidthSharePercentage = bandwidthSharePercentage;

        try {
            startEmbeddedRouter(extendedI2pLogging);
        } catch (IOException e) {
            log.error("I2P Embedded router failed to start... Please check the logs.");
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadName.set(this, "shutdownHook");
            shutdown();
        }));

        Scheduler.run(this::checkRouterStats)
                .host(this)
                .runnableName("checkRouterStats")
                .periodically(30, TimeUnit.SECONDS);

    }

    @SuppressWarnings("SpellCheckingInspection")
    public I2PSocketManager getManager(File privKeyFile) throws IOException {

        I2PSocketManager manager;
        //Has the router been initialized?
        while (RouterContext.listContexts().isEmpty()) {
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        routerContext = RouterContext.listContexts().get(0);
        router = routerContext.router();
        // Check for RUNNING state (indicating NetDB and tunnels are ready)
        while(!router.isRunning()) {
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
                if (!router.isAlive()) {
                    log.error("Router died while starting");
                    throw new IOException("Router died while starting");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("Trying to create socket manager ...");

        try (FileInputStream privKeyInputStream = new FileInputStream(privKeyFile)) {
            manager = I2PSocketManagerFactory.createManager(privKeyInputStream);
        }

        log.info("Socket manager created");

        return manager;

    }

    public void shutdown() {
        // If using embedded router, shut it down
        if (router != null) {
            router.shutdown(1);
        }
    }

    public void checkRouterStats() {
        if(routerContext==null)
            return; // Router not yet established
        CommSystemFacade.Status latestStatus = getRouterStatus();
        log.trace("I2P Router Status changed to: {}", i2pRouterStatus.name());
        if(i2pRouterStatus != latestStatus) {
            // Status changed
            i2pRouterStatus = latestStatus;
            log.info("I2P Router Status changed to: {}", i2pRouterStatus.name());
            reportRouterStatus();
        }
    }

    public static boolean isRouterRunning() {
        if(null == localRouter) {
            return false;
        }
        return localRouter.getRouter().isRunning();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("SpellCheckingInspection")
    private void startEmbeddedRouter(boolean extendedI2pLogging) throws IOException {
        I2PSocketManager manager = null;
        System.setProperty("I2P_DISABLE_OUTPUT_OVERRIDE", "true");

        Properties p = getPropertiesForEmbeddedRouter();

        try {

            //Check for running routers on the JVM, if none, create.
            router = new Router(p);
            if(extendedI2pLogging) {
                router.getContext().setLogManager(new I2PLogManager());
            }
            router.setKillVMOnEnd(false);
            router.runRouter();
            routerContext = RouterContext.listContexts().get(0);
            while(!router.isRunning()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                    if (!router.isAlive()) {
                        log.error("Router died while starting");
                        throw new IOException("Router died while starting");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            log.info("Embedded router is running ...");

            log.trace("===========Begin Router Info===========\n{}\n===========End Router Info===========", router.getRouterInfo().toString());
        }
        catch (IllegalStateException e) {
            log.error("Exception", e);
        }
    }

    private Properties getPropertiesForEmbeddedRouter() throws IOException {
        Properties p = new Properties();
        String i2pDirBasePath = dirPath + "/i2p-dir-base";
        p.put("i2p.dir.base", i2pDirBasePath);
        Files.createDirectories(Path.of(i2pDirBasePath));

        // Contains the I2P data files
        String i2pDirConfig = dirPath + "/i2p-dir-config";
        p.put("i2p.dir.config", i2pDirConfig);
        Files.createDirectories(Path.of(i2pDirConfig));

        //Parameters related to router size and badwidth share
        p.put("i2np.bandwidth.inboundKBytesPerSecond", inboundKBytesPerSecond);
        p.put("i2np.bandwidth.inboundBurstKBytesPerSecond", inboundKBytesPerSecond);
        p.put("i2np.bandwidth.outboundKBytesPerSecond", outboundKBytesPerSecond);
        p.put("i2np.bandwidth.outboundBurstKBytesPerSecond", outboundKBytesPerSecond);
        p.put("router.sharePercentage", bandwidthSharePercentage);

        p.put("i2cp.disableInterface", "true");
        p.put("i2np.ntcp.nodelay", "true");
        p.put("router.encType","4");
        p.put("router.useShortTBM","true");

        // Copy reseed certificates
        String embeddedRouterCertPath = i2pDirBasePath + "/certificates/reseed";
        Files.createDirectories(Path.of(embeddedRouterCertPath));
        // Retrieved from https://github.com/i2p/i2p.i2p/tree/master/installer/resources/certificates/reseed
        // Saved under 'resources/embedded/certificates/reseed/'
        for (String s : List.of(
                "creativecowpat_at_mail.i2p.crt",
                "echelon3_at_mail.i2p.crt",
                "hankhill19580_at_gmail.com.crt",
                "hottuna_at_mail.i2p.crt",
                "igor_at_novg.net.crt",
                "lazygravy_at_mail.i2p.crt",
                "rambler_at_mail.i2p.crt",
                "reseed_at_diva.exchange.crt")) {
            Files.copy(
                    Objects.requireNonNull(getClass().getResourceAsStream("/embedded/certificates/reseed/" + s)),
                    Paths.get(embeddedRouterCertPath, s),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        return p;
    }

    private CommSystemFacade.Status getRouterStatus() {
        return routerContext.commSystem().getStatus();
    }

    private void reportRouterStatus() {
        if(i2pRouterStatus == CommSystemFacade.Status.OK ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_DISABLED_IPV6_OK ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_FIREWALLED_IPV6_OK ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_SNAT_IPV6_OK ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_UNKNOWN_IPV6_OK ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_OK_IPV6_UNKNOWN ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_DISABLED_IPV6_FIREWALLED ||
                i2pRouterStatus == CommSystemFacade.Status.REJECT_UNSOLICITED ||
                i2pRouterStatus == CommSystemFacade.Status.IPV4_OK_IPV6_FIREWALLED) {
            log.info("I2P Router status - {}", i2pRouterStatus.toStatusString());
        }
        else if(i2pRouterStatus == CommSystemFacade.Status.DIFFERENT ||
                i2pRouterStatus == CommSystemFacade.Status.HOSED) {
            log.warn("I2P Router status - {}", i2pRouterStatus.toStatusString());
        }
        else if(i2pRouterStatus == CommSystemFacade.Status.DISCONNECTED) {
            log.warn("I2P Router status - {}", i2pRouterStatus.toStatusString());
            //todo - create a restart() routine?
        }
        else {
            log.warn("Not connected to I2P Network.");
        }
    }
}
