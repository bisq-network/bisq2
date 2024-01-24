package bisq.persistence;

import lombok.Getter;

import java.io.File;

@Getter
public enum DbSubDirectory {
    NETWORK_DB("network_db"),  // Used for dataStores which persist data from the network data storage
    CACHE("cache"),            // Used for dataStores which persist data derived from the network
    SETTINGS("settings"),      // Used for dataStores which persist user setting data
    PRIVATE("private"),        // Used for dataStores which persist private user data
    WALLETS("wallets");        // Used for dataStores which persist wallets

    private final String dbPath;

    DbSubDirectory(String subDir) {
        this.dbPath = "db" + File.separator + subDir;
    }
}
