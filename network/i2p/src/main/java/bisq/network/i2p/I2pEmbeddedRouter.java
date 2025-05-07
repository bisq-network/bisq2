package bisq.network.i2p;

import bisq.common.threading.ThreadName;
import bisq.common.timer.Scheduler;
import bisq.network.i2p.util.I2PLogManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PClient;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataHelper;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import net.i2p.util.FileUtil;
import net.i2p.util.Log;
import net.i2p.util.OrderedProperties;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    private File i2pDir;
    private final Integer restartAttempts=0;
    private static final Integer RESTART_ATTEMPTS_UNTIL_HARD_RESTART = 3;
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
        this.i2pDir = new File(i2pDirPath);

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

    public boolean shutdown() {
        // If using embedded router, shut it down
        if (router != null) {
            router.shutdown(1);
            return true;
        }
        return false;
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

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    @SuppressWarnings("SpellCheckingInspection")
    private void startEmbeddedRouter(boolean extendedI2pLogging) throws IOException {
        I2PSocketManager manager = null;
        System.setProperty("I2P_DISABLE_OUTPUT_OVERRIDE", "true");

       getPropertiesForEmbeddedRouter();

        try {
            log.info("Launching I2P Router...");
            RouterLaunch.main(null);
            List<RouterContext> routerContexts = RouterContext.listContexts();
            routerContext = routerContexts.get(0);
            router = routerContext.router();
            //Check for running routers on the JVM, if none, create.
           // router = new Router(p);
            if(extendedI2pLogging) {
                router.getContext().setLogManager(new I2PLogManager());
            }
            router.setKillVMOnEnd(false);
            routerContext.logManager().setDefaultLimit(Log.STR_INFO);
            routerContext.logManager().setFileSize(10_000_000);
            long startTime = System.currentTimeMillis();
            final long timeoutMs = 60_000;
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

    private void getPropertiesForEmbeddedRouter() throws IOException {

        if(System.getProperty("i2p.dir.base")==null) {
            File homeDir = SystemSettings.getUserHomeDir();
            File i2pDir = new File(homeDir, ".bisq2");
            if (!i2pDir.exists() && !i2pDir.mkdir()) {
                log.warn("Unable to create home/.bisq2 directory.");

            }
            File servicesDir = new File(i2pDir, "services");
            if (!servicesDir.exists() && !servicesDir.mkdir()) {
                log.warn("Unable to create services directory in home/.bisq2");
            }
            i2pDir = new File(servicesDir, I2pEmbeddedRouter.class.getName());
            if (!i2pDir.exists() && !i2pDir.mkdir()) {
                log.warn("Unable to create " + I2pEmbeddedRouter.class.getName() + " directory in home/.bisq2/services");
            }
        } else {
            i2pDir = new File(System.getProperty("i2p.dir.base"));
        }

        // Config Directory
        File i2pConfigDir = new File(i2pDir, "config");
        if(!i2pConfigDir.exists())
            if(!i2pConfigDir.mkdir())
                log.warn("Unable to create I2P config directory: " +i2pConfigDir);
        if(i2pConfigDir.exists()) {
            System.setProperty("i2p.dir.config",i2pConfigDir.getAbsolutePath());
            //    config.setProperty("i2p.dir.config",i2pConfigDir.getAbsolutePath());
        }
        // Router Directory
        File i2pRouterDir = new File(i2pDir,"router");
        if(!i2pRouterDir.exists())
            if(!i2pRouterDir.mkdir())
                log.warn("Unable to create I2P router directory: "+i2pRouterDir);
        if(i2pRouterDir.exists()) {
            System.setProperty("i2p.dir.router",i2pRouterDir.getAbsolutePath());
            //     config.setProperty("i2p.dir.router",i2pRouterDir.getAbsolutePath());
        }

        // PID Directory
        File i2pPIDDir = new File(i2pDir, "pid");
        if(!i2pPIDDir.exists())
            if(!i2pPIDDir.mkdir())
                log.warn("Unable to create I2P PID directory: "+i2pPIDDir.getAbsolutePath());
        if(i2pPIDDir.exists()) {
            System.setProperty("i2p.dir.pid",i2pPIDDir.getAbsolutePath());
            //    config.setProperty("i2p.dir.pid",i2pPIDDir.getAbsolutePath());
        }
        // Log Directory
        File i2pLogDir = new File(i2pDir,"log");
        if(!i2pLogDir.exists())
            if(!i2pLogDir.mkdir())
                log.warn("Unable to create I2P log directory: "+i2pLogDir.getAbsolutePath());
        if(i2pLogDir.exists()) {
            System.setProperty("i2p.dir.log",i2pLogDir.getAbsolutePath());
            //   config.setProperty("i2p.dir.log",i2pLogDir.getAbsolutePath());
        }
        // App Directory
        File i2pAppDir = new File(i2pDir,"app");
        if(!i2pAppDir.exists())
            if(!i2pAppDir.mkdir())
                log.warn("Unable to create I2P app directory: "+i2pAppDir.getAbsolutePath());
        if(i2pAppDir.exists()) {
            System.setProperty("i2p.dir.app", i2pAppDir.getAbsolutePath());
            //   config.setProperty("i2p.dir.app", i2pAppDir.getAbsolutePath());
        }
        System.setProperty(I2PClient.PROP_TCP_HOST, "internal");
        System.setProperty(I2PClient.PROP_TCP_PORT, "internal");
        System.setProperty("i2p.dir.base", i2pDir.getAbsolutePath());
        mergeRouterConfig(null);

        File certDir = new File(i2pDir, "certificates");
        if(!certDir.exists())
            if(!certDir.mkdir()) {
                log.warn("Unable to create certificates directory in: "+certDir.getAbsolutePath()+"; exiting...");
            }
        File seedDir = new File(certDir, "reseed");
        if(!seedDir.exists())
            if(!seedDir.mkdir()) {
                log.warn("Unable to create "+seedDir.getAbsolutePath()+" directory; exiting...");
            }
        File sslDir = new File(certDir, "ssl");
        if(!sslDir.exists())
            if(!sslDir.mkdir()) {
                log.warn("Unable to create "+sslDir.getAbsolutePath()+" directory; exiting...");
            }

        File seedCertificates = new File(certDir, "reseed");
        File sslCertificates = new File(certDir, "ssl");

        copyCertificatesToBaseDir(seedCertificates, sslCertificates);
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
    }

    private CommSystemFacade.Status getRouterStatus() {
        return routerContext.commSystem().getStatus();
    }

    /**
     *  Load defaults from internal router.config on classpath,
     *  then add props from i2pDir/router.config overriding any from internal router.config,
     *  then override these with the supplied overrides if not null which would likely come from 3rd party app (not yet supported),
     *  then write back to i2pDir/router.config.
     *
     *  @param overrides local overrides or null
     */
    private void mergeRouterConfig(Properties overrides) {
        Properties props = new OrderedProperties();
        File f = new File(i2pDir, "router.config");
        boolean i2pBaseRouterConfigIsNew = false;
        if (!f.exists()) {
            if (!f.mkdir()) {
                log.warn("While merging router.config files, unable to create router.config in i2pBaseDirectory: " + i2pDir.getAbsolutePath());
            } else {
                i2pBaseRouterConfigIsNew = true;
            }
        }
        InputStream i2pBaseRouterConfig = null;
        try {
            props.putAll(loadFromClasspath("router.config"));

            if (!i2pBaseRouterConfigIsNew) {
                i2pBaseRouterConfig = new FileInputStream(f);
                DataHelper.loadProps(props, i2pBaseRouterConfig);
            }

            // override with user settings
            if (overrides != null)
                props.putAll(overrides);

            DataHelper.storeProps(props, f);
        } catch (Exception e) {
            log.warn("Exception caught while merging router.config properties: " + e.getLocalizedMessage());
        } finally {
            if (i2pBaseRouterConfig != null) try {
                i2pBaseRouterConfig.close();
            } catch (IOException ioe) {
            }
        }
    }

    private Properties loadFromClasspath(String resourcePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }
            props.load(in);
        }
        return props;
    }

    public File getDirectory() {
        return i2pDir;
    }

    /**
     *  Copy all certificates found in certificates on classpath
     *  into i2pDir/certificates
     *
     *  @param reseedCertificates destination directory for reseed certificates
     *  @param sslCertificates destination directory for ssl certificates
     */
    private boolean copyCertificatesToBaseDir(File reseedCertificates, File sslCertificates) {
        String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        final File jarFile = new File(jarPath);
        if (jarFile.isFile()) {
            try {
                final JarFile jar = new JarFile(jarFile);
                JarEntry entry;
                File f = null;
                final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();
                    final String name = entry.getName();
                    if (name.startsWith("certificates/reseed/")) { //filter according to the path
                        if (!name.endsWith("/")) {
                            String fileName = name.substring(name.lastIndexOf("/") + 1);
                            log.info("fileName to save: " + fileName);
                            f = new File(reseedCertificates, fileName);
                        }
                    }
                    if (name.startsWith("certificates/ssl/")) {
                        if (!name.endsWith("/")) {
                            String fileName = name.substring(name.lastIndexOf("/") + 1);
                            log.info("fileName to save: " + fileName);
                            f = new File(sslCertificates, fileName);
                        }
                    }
                    if (f != null) {
                        boolean fileReadyToSave = false;
                        if (!f.exists() && f.createNewFile()) fileReadyToSave = true;
                        else if (f.exists() && f.delete() && f.createNewFile()) fileReadyToSave = true;
                        if (fileReadyToSave) {
                            FileOutputStream fos = new FileOutputStream(f);
                            byte[] byteArray = new byte[1024];
                            int i;
                            InputStream is = getClass().getClassLoader().getResourceAsStream(name);
                            //While the input stream has bytes
                            while ((i = is.read(byteArray)) > 0) {
                                //Write the bytes to the output stream
                                fos.write(byteArray, 0, i);
                            }
                            //Close streams to prevent errors
                            is.close();
                            fos.close();
                            f = null;
                        } else {
                            log.warn("Unable to save file from 1M5 jar and is required: " + name);
                            return false;
                        }
                    }
                }
                jar.close();
            } catch (IOException e) {
                log.warn(e.getLocalizedMessage());
                return false;
            }
        } else {
            // called while testing in an IDE
            URL resource = I2pEmbeddedRouter.class.getClassLoader().getResource(".");
            File file = null;
            try {
                file = new File(resource.toURI());
            } catch (URISyntaxException e) {
                log.warn("Unable to access I2P resource directory.");
                return false;
            }
            File[] resFolderFiles = file.listFiles();
            File certResFolder = null;
            for (File f : resFolderFiles) {
                if ("certificates".equals(f.getName())) {
                    certResFolder = f;
                    break;
                }
            }
            if (certResFolder != null) {
                File[] folders = certResFolder.listFiles();
                for (File folder : folders) {
                    if ("reseed".equals(folder.getName())) {
                        File[] reseedCerts = folder.listFiles();
                        for (File reseedCert : reseedCerts) {
                            FileUtil.copy(reseedCert, reseedCertificates, true, false);
                        }
                    } else if ("ssl".equals(folder.getName())) {
                        File[] sslCerts = folder.listFiles();
                        for (File sslCert : sslCerts) {
                            FileUtil.copy(sslCert, sslCertificates, true, false);
                        }
                    }
                }
                return true;
            }
            return false;
        }

        return true;
    }

    public void restart() {
        if(router==null) {
            router = routerContext.router();
            if(router==null) {
                log.warn("Unable to restart I2P Router. Router instance not found in RouterContext.");
            }
        } else {
            log.info("Soft restart of I2P Router...");
            router.restart();
            int maxWaitSec = 10 * 60; // 10 minutes
            int currentWait = 0;
            while(!routerContext.router().isAlive()) {
                currentWait+=10;
                if(currentWait > maxWaitSec) {
                    log.warn("Restart failed.");
                    return ;
                }
            }
            log.info("Router hiddenMode="+router.isHidden());
            log.info("I2P Router soft restart completed.");
        }
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
            restart();
        }
        else {
            log.warn("Not connected to I2P Network.");
        }
    }
}
