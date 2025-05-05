# Running the Bisq App Over I2P and TOR

Once a seed node is established, this manual setup process will no longer be necessary, as Bisq will automatically connect via an active I2P destination. Until then, follow the steps below to run the Bisq desktop app with I2P and/or TOR support.

---

## Prerequisites

- Java installed
- Local I2P router running
- Project built using Gradle

---

## 1. Build the Project

```bash
./gradlew :apps:desktop:desktop-app:installDist
```

---

## 2. Obtain an Active I2P Address

To simulate a seed node:

- Run `EchoServer.java` while your local I2P router is active.
- This will generate a Base64-encoded I2P destination address.
- Use this address temporarily as your seed node in the application launch configuration.

---

## 3. Run the Desktop App

### ▶️ Using I2P Only

```bash
JAVA_OPTS="-Dapplication.appName=bisq2_Alice_i2p -Dapplication.network.supportedTransportTypes.0=I2P -Dapplication.network.seedAddressByTransportType.i2p.0=agBmOUKz36nwicE~cS8imtyTWhdu3fGA~9breLENDFHNByyD94-7vsMj4~0tY1GOOv0h8GNtTNWWo0RFR0S6BLkRv9QFldKmWNR~QnDHHVSnshgxzxZrawp6UsiAIUqFQ586dsH3M6x8LTHb1yEhC4~X0uDN481oA6CHs0VpsN1BzM90F9e4yQhcnSTKEeucy0QtOTkETtjqiFTmApKm~z6fQZ4aZyfxH7rCZs~HEKhJKM1-Qf~JAtfLU3ISstNP445-ecDB18gwnLQliOAk5Q3NK3vgoBt~bzPudiWTF5VN17XZVPorl0xIQhYwT5gMWCNFZlMam~gNlMCaYRXQWAlbJkGvuSzz~xWwbni0dqj2iZArAF8E10cEwOSEmt0x2kElQtTQyqyEXyooNZcQ8F1DOtd1-u-vK5SazaqYdxgW~d5K3ZIBJ0bP8hQaoCK24gal~Z~~WvyCinrmdUHHmUeA2dYAe0WIECQbgpHIG16cMMQptLcBzN1A3XsBxIqaAAAA:1234" apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

### ▶️ Using I2P + TOR

```bash
JAVA_OPTS="-Dapplication.appName=bisq2_Boy_i2p -Dapplication.network.supportedTransportTypes.0=I2P -Dapplication.network.supportedTransportTypes.1=TOR -Dapplication.network.seedAddressByTransportType.i2p.0=5HCRWTJGEEvAERcmDRs2XlaXwwi845ZGOl6fVyHeVQoVAj0HPyJ2ahIXtp3jPUOiMaiTPebfLekpGYdOCRcWTun1zg0wwBqcuw7T1QZ~mQaZis3v6ypFxF2XPmjpQn5cPHOlxdHwR-F8QWmKHe6yrV7-8oSGElf0~SQ6LBsCQo125mpjPNa3JNdfvVpxIgCPpoTikpwBMlT44uq1-h8EWd0nv33JKHQlz1d6ZQKVArd8OeEy3fCZxI~B-JuT1lz8ddnzVtgeJXqDNjnHYMZaPqxYl0HiW5-WgFqtpjwot76rRzS2z5v62xB3~H6DOkAhfqWR0hZU5NJ0r3FJzfTcUVa8njJqmHnOuF7JmJsT3oYi5xLjyQjwJfaC8fx7CO16FTlkmabamhU9eoZd4SJ1F2NujGrTemybFrcUi4tgIyjRdse6L3Uu6R26rKrScoWVDgqQG5XgDUs4kR8nTDJg0BBBQEDbsZDqtP-NLam~yEBNP~NIrlylRspDrp1ccsliAAAA:53886 -Dapplication.network.seedAddressByTransportType.tor.1=ctcce2hieq3v2tcmeaapwfbnoc6s546rsqbd3etfsmxx5zdekigyhtyd.onion:31873" apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

---

## 4. Reuse Newly Generated Addresses

Once the application is running:

- Navigate to the Network Settings in the desktop app.
- Copy the newly generated I2P or TOR address.
- Replace the seed address in your `JAVA_OPTS` with this new address.
- Repeat Step 3 with the updated configuration.

---

## 
> **Note:** Sometimes I2P can take longer to initialize and publish the address. If the application does not start successfully, try waiting a few minutes, or restart the app.

5. Start Using Bisq Over I2P

Once the connection is established:

- Bisq will operate over I2P and/or TOR as configured.
- You are now running a privacy-preserving instance of Bisq.
