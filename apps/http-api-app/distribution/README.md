# Bisq Trusted Node

**Version:** 2.1.7  
**For:** Bisq Connect Mobile App (Android & iOS)

---

## 📋 **What is This?**

This is a **Bisq Trusted Node** - a lightweight server that allows your Bisq Connect mobile app to connect to the Bisq network. 

You run this on your own computer/server, and your mobile app connects to it securely via:
- **Tor** (recommended - works anywhere, fully private)
- **Local Network** (LAN - faster, for home use)

You can setup Auth as well which is optional but strongly recommended specially if you are sharing your trusted node 
outside your LAN.

---

## ⚡ **Quick Start**

### **Prerequisites**

- **Java 22.0.2 or higher** installed
  - Check: `java -version` (should show version 22.0.2 or higher)
  - Bisq2 is tested with Java 22.0.2.fx-zulu
  - Download: [Adoptium](https://adoptium.net/) or [Azul Zulu FX](https://www.azul.com/downloads/?package=jdk-fx)

### **Step 1: Extract**

```bash
# Extract the archive
unzip bisq-trusted-node-2.1.7.zip
# or
tar -xzf bisq-trusted-node-2.1.7.tar.gz

cd bisq-trusted-node-2.1.7
```

### **Step 2: Configure**

Edit `trusted-node.properties` and set your configuration:

```properties
# Instance name - determines the data directory and profile
appName=bisq2_http_prod

# WebSocket password - REQUIRED for mobile app authentication
password=YOUR_STRONG_PASSWORD

# WebSocket server port (default: 8090)
port=8090

# Development mode - keep as false for production
devMode=false

# Network transport types (TOR for remote, CLEAR for LAN)
transportTypes=TOR,CLEAR
```

**Important settings:**
- **`appName`** - Unique name for this instance (each mobile user needs their own)
- **`password`** - Strong password for mobile app authentication (REQUIRED!)
- **`port`** - WebSocket port (change if running multiple instances)
- **`transportTypes`** - `TOR` for remote access, `CLEAR` for LAN

### **Step 3: Run**

**Linux/macOS:**
```bash
./run-trusted-node.sh
```

**Windows:**
```cmd
run-trusted-node.bat
```

**The node will:**
1. Read your configuration from `trusted-node.properties`
2. Bootstrap Tor (takes 2-5 minutes first time)
3. Connect to the Bisq network
4. Generate a Tor onion address
5. Start WebSocket server on your configured port

---

## 📱 **Connect Your Mobile App**

## **Option 1: Tor Connection (Recommended)**

1. **Find your Tor onion address:**

   **Linux/macOS:**
   ```bash
   cat ~/.local/share/bisq2_http_prod/webSocketServer_onionAddress.txt
   ```
   # macOS (latest versions)
   ```bash
   cat ~/Library/Application\ Support/bisq2_http_prod/webSocketServer_onionAddress.txt
   ```

   **Windows:**
   ```cmd
   type %USERPROFILE%\.local\share\bisq2_http_prod\webSocketServer_onionAddress.txt
   ```

   Example output: `abc123xyz456.onion:8090`

2. **In Bisq Connect app:**
   - Go to Settings → Trusted Node
   - Enter: `ws://abc123xyz456.onion:8090`
   - Tap on "advanced options" and enter your password in the password input
   - Tap "Test & Save"

## **Option 2: Local Network (LAN)**

1. **Find your local IP address:**

   **Linux:**
   ```bash
   hostname -I | awk '{print $1}'
   ```

   **macOS:**
   ```bash
   ipconfig getifaddr en0
   ```

   **Windows:**
   ```cmd
   ipconfig
   ```
   (Look for "IPv4 Address")

2. **In Bisq Connect app:**
   - Make sure your phone is on the same WiFi network
   - Go to Settings → Trusted Node
   - Enter: `YOUR_LOCAL_IP:8090` (if port is 8090 the ":8090" can be omitted)
   - Enter your password in advanced settings
   - Tap "Test & Save"

---

## ⚙️ **Advanced Configuration**

### **Run Multiple Instances**

To support multiple mobile users on the same server, create separate properties files:

**1. Copy the properties file for each user:**
```bash
cp trusted-node.properties alice.properties
cp trusted-node.properties bob.properties
```

**2. Edit each file with unique settings:**

`alice.properties`:
```properties
appName=bisq2_alice
password=alice_strong_password
port=8090
devMode=false
transportTypes=TOR,CLEAR
```

`bob.properties`:
```properties
appName=bisq2_bob
password=bob_strong_password
port=8091
devMode=false
transportTypes=TOR,CLEAR
```

**3. Run each instance (in separate terminals):**
```bash
# Alice's instance
PROPERTIES_FILE=alice.properties ./run-trusted-node.sh

# Bob's instance
PROPERTIES_FILE=bob.properties ./run-trusted-node.sh
```

Each instance will have its own:
- Data directory (`~/.local/share/bisq2_alice`, `~/.local/share/bisq2_bob`)
- Tor onion address
- Network identity

### **Manual Configuration (Without Properties File)**

If you prefer to set configuration via command line:

```bash
JAVA_OPTS="-Dapplication.appName=bisq2_http_prod \
           -Dapplication.websocket.password=YOUR_PASSWORD \
           -Dapplication.devMode=false \
           -Dapplication.network.supportedTransportTypes.0=TOR \
           -Dapplication.network.supportedTransportTypes.1=CLEAR \
           -Dapplication.websocket.server.port=8090" \
./bin/http-api-app
```

**What each setting does:**
- **`appName`** - Instance name (determines data directory)
- **`websocket.password`** - Password for mobile app authentication
- **`devMode`** - Must be `false` for production
- **`supportedTransportTypes.0`** - First transport (TOR)
- **`supportedTransportTypes.1`** - Second transport (CLEAR)
- **`websocket.server.port`** - WebSocket port

---

## 🐧 **Running as a Service (Linux)**

For 24/7 operation, run as a systemd service:

### **Create Service File**

```bash
sudo nano /etc/systemd/system/bisq-trusted-node.service
```

**Paste this:**

```ini
[Unit]
Description=Bisq Trusted Node
After=network.target

[Service]
Type=simple
User=YOUR_USERNAME
WorkingDirectory=/home/YOUR_USERNAME/bisq-trusted-node-2.1.7
Environment="JAVA_OPTS=-Dapplication.appName=bisq2_http_prod -Dapplication.websocket.password=YOUR_PASSWORD"
ExecStart=/home/YOUR_USERNAME/bisq-trusted-node-2.1.7/bin/http-api-app
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Replace:**
- `YOUR_USERNAME` with your Linux username
- `YOUR_PASSWORD` with your chosen password
- Adjust paths if needed

### **Enable and Start**

```bash
sudo systemctl daemon-reload
sudo systemctl enable bisq-trusted-node.service
sudo systemctl start bisq-trusted-node.service
```

### **Check Status**

```bash
sudo systemctl status bisq-trusted-node.service
sudo journalctl -u bisq-trusted-node.service -f
```

---

## 📂 **Data Locations**

### **Linux/macOS:**
- Data: `~/.local/share/bisq2_http_prod/`
- Logs: `~/.local/share/bisq2_http_prod/bisq_2.log`
- Tor: `~/.local/share/bisq2_http_prod/tor/`
- Onion address: `~/.local/share/bisq2_http_prod/webSocketServer_onionAddress.txt`

**Note**: Replace .local/share with Library/Application Support on latest macOS versions

### **Windows:**
- Data: `%USERPROFILE%\.local\share\bisq2_http_prod\`
- Logs: `%USERPROFILE%\.local\share\bisq2_http_prod\bisq_2.log`
- Tor: `%USERPROFILE%\.local\share\bisq2_http_prod\tor\`
- Onion address: `%USERPROFILE%\.local\share\bisq2_http_prod\webSocketServer_onionAddress.txt`

---

## 🔥 **Firewall Configuration**

### **For LAN Access:**

Allow incoming connections on port 8090 (or your custom port):

**Linux (ufw):**
```bash
sudo ufw allow 8090/tcp
```

**Linux (firewalld):**
```bash
sudo firewall-cmd --permanent --add-port=8090/tcp
sudo firewall-cmd --reload
```

**macOS:**
```bash
# System Preferences → Security & Privacy → Firewall → Firewall Options
# Allow incoming connections for "http-api-app"
```

**Windows:**
```cmd
netsh advfirewall firewall add rule name="Bisq Trusted Node" dir=in action=allow protocol=TCP localport=8090
```

### **For Tor Access:**

No firewall configuration needed! Tor handles everything.
Note that if you have a system level tor instance running it could conflict with the tor instance of the bisq trusted node.

---

## 🐛 **Troubleshooting**

### **"Tor bootstrap timeout"**

**Solution:** Increase timeout:
```bash
JAVA_OPTS="-Dapplication.network.configByTransportType.tor.bootstrapTimeout=600" \
./bin/http-api-app
```

or alternatively in the http_api_app.conf file:
```properties
application.network.configByTransportType.tor.bootstrapTimeout=600
```

and rebuild.

### **"Cannot find Tor binary"**

**Linux only:** Install system Tor:
```bash
sudo apt install tor
sudo systemctl stop tor
sudo systemctl disable tor
```

### **"Port 8090 already in use"**

**Solution:** Change the port:
```bash
JAVA_OPTS="-Dapplication.websocket.server.port=9000" \
./bin/http-api-app
```

### **"Mobile app cannot connect"**

**Check:**
1. Trusted node is running: `ps aux | grep http-api-app`
2. Onion address file exists and has content
3. Password matches in mobile app
4. For LAN: Phone and server on same network
5. For LAN: Firewall allows port 8090

**View logs:**
```bash
# Linux/macOS
tail -f ~/.local/share/bisq2_http_prod/bisq_2.log

# Windows
type %USERPROFILE%\.local\share\bisq2_http_prod\bisq_2.log
```

---

## 📊 **System Requirements**

- **OS:** Linux, macOS, or Windows
- **Java:** 22.0.2 or higher (tested with Java 22.0.2.fx-zulu)
- **RAM:** 2 GB minimum, 4 GB recommended
- **Disk:** 500 MB for application + data
- **Network:** Internet connection (for Tor). Although possible, we don't recommend using CLEARNET outside your LAN.

---

## 🔄 **Updates**

To update to a new version:

1. Stop the current node
2. Download the new version
3. Extract to a new directory
4. Run the new version with the same `appName`

Your data is preserved in `~/.local/share/bisq2_http_prod/`

---

## 📞 **Support**

- **Bisq Mobile GitHub:** [bisq-network/bisq-mobile](https://github.com/bisq-network/bisq-mobile)
- **Bisq2 GitHub:** [bisq-network/bisq2](https://github.com/bisq-network/bisq2)
- **Matrix Chat:** [#bisq:bitcoin.kyoto](https://matrix.to/#/#bisq:bitcoin.kyoto)
- **Forum:** [bisq.community](https://bisq.community/)

---

## 📜 **License**

AGPL v3 - See LICENSE file

---

## ⚠️ **Important Notes**

1. **One user per instance:** Each mobile user needs their own trusted node instance with a unique `appName` and port.
2. **Password security:** Use a strong password and keep it safe to share only with the mobile user you intend it to use your trusted node.
3. **Tor privacy:** Tor connections are fully private and work from anywhere
4. **LAN speed:** LAN connections are faster but only work on your local network
5. **Version compatibility:** Trusted node version must match your mobile app version. Mobile app will demand upgrade of trusted node if needed.

---

**Enjoy using Bisq Connect!** 🚀

