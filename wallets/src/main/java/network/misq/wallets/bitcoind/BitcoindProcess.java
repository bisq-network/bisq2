package network.misq.wallets.bitcoind;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.FileUtils;
import network.misq.wallets.NetworkType;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import network.misq.wallets.bitcoind.rpc.RpcClient;
import network.misq.wallets.bitcoind.rpc.RpcConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

@Slf4j
public class BitcoindProcess {

    private final RpcConfig rpcConfig;
    private final Path dataDir;

    private Process bitcoindProcess;

    public BitcoindProcess(RpcConfig rpcConfig, Path dataDir) {
        this.rpcConfig = rpcConfig;
        this.dataDir = dataDir;
    }

    public void startAndWaitUntilReady() throws IOException {
        FileUtils.makeDirs(dataDir.toFile());
        bitcoindProcess = createAndStartProcess();
        waitUntilReady();
    }

    public boolean stopAndWaitUntilStopped() {
        try {
            invokeStopRpcCall();
            bitcoindProcess.waitFor();
            return true;

        } catch (IOException | InterruptedException e) {
            if (e instanceof IOException) {
                log.error("Cannot send stop rpc call to bitcoind.", e);
            }

            if (e instanceof InterruptedException) {
                log.error("Cannot wait until bitcoind terminated.", e);
            }
            return false;
        }
    }

    private Process createAndStartProcess() throws IOException {
        String networkArg = getBitcoindParamForNetworkType(rpcConfig.networkType());
        return new ProcessBuilder(
                "bitcoind",
                networkArg,
                "-datadir=" + dataDir.toAbsolutePath(),
                "-whitelist=127.0.0.1",
                "-rpcbind",
                "-rpcallowip=" + rpcConfig.hostname(),
                "-rpcuser=" + rpcConfig.user(),
                "-rpcpassword=" + rpcConfig.password(),
                "-fallbackfee=0.00000001",
                "-txindex=1"
        ).start();
    }

    private void waitUntilReady() {
        waitUntilLogFileContainsLine("init message: Done loading");
    }

    private void waitUntilLogFileContainsLine(String lineToMatch) {
        try (Scanner scanner = new Scanner(bitcoindProcess.getInputStream())) {
            while (scanner.hasNextLine()) {
                // The bitcoind log starts with a timestamp, so we check only for expected string with `endsWith`
                if (scanner.nextLine().endsWith(lineToMatch)) {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Bitcoind didn't start correctly.", e);
            throw e;
        }
    }

    private void invokeStopRpcCall() throws IOException {
        try {
            var chainBackend = new BitcoindChainBackend(new RpcClient(rpcConfig));
            chainBackend.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to bitcoind.", e);
        }
    }

    private String getBitcoindParamForNetworkType(NetworkType networkType) {
        return switch (networkType) {
            case MAINNET -> "";
            case REGTEST -> "-regtest";
            case SIGNET -> "-signet";
            case TESTNET -> "-testnet";
        };
    }
}
