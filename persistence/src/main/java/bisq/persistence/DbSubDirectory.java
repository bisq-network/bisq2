package bisq.persistence;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public enum DbSubDirectory {
    NETWORK_DB("network_db"),  // Used for dataStores which persist data from the network data storage
    CACHE("cache"),            // Used for dataStores which persist data derived from the network
    SETTINGS("settings"),      // Used for dataStores which persist user setting data
    PRIVATE("private");        // Used for dataStores which persist private user data

    private final Path dbPath;

    DbSubDirectory(String subDir) {
        this.dbPath = Path.of("db", subDir);
    }
}
