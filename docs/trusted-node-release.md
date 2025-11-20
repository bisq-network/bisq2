# Bisq Trusted Node - Release Guide

This guide explains how to build and release the Bisq Trusted Node distribution packages for Bisq Connect mobile app users.

---

## 📦 **What Was Created**

### **1. Distribution Package Structure**

The build creates a complete, standalone distribution:

```
bisq-trusted-node/
├── bin/
│   ├── http-api-app          # POSIX launcher script
│   └── http-api-app.bat      # Windows launcher script
├── lib/
│   ├── http-api-app.jar      # Main application
│   ├── network-2.1.7.jar     # Network layer
│   ├── tor.jar               # Embedded Tor
│   └── ... (125+ dependency JARs, ~76 MB total)
├── README.md                  # Complete user guide
├── LICENSE                    # AGPL v3
└── CHANGELOG.md              # Version history
```

### **2. Build Artifacts**

- **ZIP**: `bisq-trusted-node.zip` (~76 MB) - Works on all platforms
- **TAR.GZ**: `bisq-trusted-node.tar.gz` (~76 MB) - Preferred for Linux/macOS

### **3. Key Features**

✅ **No build required** - Users just extract and run  
✅ **Includes all dependencies** - Only Java 22 needed  
✅ **Cross-platform** - Windows, Linux, macOS  
✅ **Comprehensive docs** - README with all setup instructions  
✅ **Password protection** - WebSocket authentication  
✅ **Multi-instance support** - Multiple users per server  
✅ **Tor + Clearnet** - Both connection types supported  

---

## 🚀 **How to Build a Release**

### **Quick Build (Automated)**

```bash
# Build both ZIP and TAR.GZ with version from gradle.properties (RECOMMENDED)
./build-trusted-node-release.sh

# Or specify a custom version
./build-trusted-node-release.sh 2.1.8
```

This script will:
1. Clean previous builds
2. Build ZIP and TAR.GZ distributions
3. Generate SHA256 checksums
4. Create release notes
5. Organize everything in `releases/trusted-node-v{VERSION}/`

### **Manual Build**

```bash
# Build ZIP only
./gradlew :apps:http-api-app:distZip -x test

# Build TAR.GZ only
./gradlew :apps:http-api-app:distTar -x test

# Build both
./gradlew :apps:http-api-app:distZip :apps:http-api-app:distTar -x test
```

Artifacts will be in: `apps/http-api-app/build/distributions/`

---

## 📋 **Release Checklist**

### **Before Building**

- [ ] Update version in `gradle.properties`
- [ ] Update `apps/http-api-app/distribution/README.md` with new version
- [ ] Update `apps/http-api-app/distribution/CHANGELOG.md` with changes
- [ ] Test the latest code (run the app locally)
- [ ] Ensure all critical fixes are included (e.g., null address fix)

### **Building**

- [ ] Run `./build-trusted-node-release.sh`
- [ ] Verify build completed successfully
- [ ] Check file sizes are reasonable (~76 MB)
- [ ] Verify SHA256SUMS.txt was generated

### **Testing**

- [ ] Extract the ZIP on your platform
- [ ] Run `./bin/http-api-app` (or `.bat` on Windows)
- [ ] Verify Tor bootstraps successfully
- [ ] Check onion address file is created
- [ ] Test WebSocket connection with password
- [ ] Test LAN connection
- [ ] Verify logs show no errors

### **Releasing**

- [ ] Create GitHub release in `bisq-mobile` repo
- [ ] Tag: `trusted-node-v{VERSION}`
- [ ] Title: `Bisq Trusted Node v{VERSION}`
- [ ] Upload `bisq-trusted-node-{VERSION}.zip`
- [ ] Upload `bisq-trusted-node-{VERSION}.tar.gz`
- [ ] Upload `SHA256SUMS.txt`
- [ ] Copy release notes from `RELEASE_NOTES.md`
- [ ] Mark as pre-release if beta
- [ ] Publish release

### **After Release**

- [ ] Update bisq-mobile wiki with new version
- [ ] Announce in Matrix/Forum
- [ ] Update mobile app to reference new version
- [ ] Monitor for user issues
- [ ] Regenerate bisq-mobile development branch for next iteration. 
E.g. if version `2.1.8` was released and next version is `2.1.9` then create bisq2 branch `for-mobile-based-on-2.1.8`
for mobile devs to continue working.

---

## 🔧 **Configuration Files**

### **README.md** (`apps/http-api-app/distribution/README.md`)

This is the main user-facing documentation. It includes:
- Quick start guide
- Password setup instructions
- How to find Tor onion address (per platform)
- Port configuration
- Instance name (appName) explanation
- Multi-instance setup
- Systemd service example
- Firewall configuration
- Troubleshooting
- Data locations per platform

**Update this when:**
- Version changes
- New features added
- Configuration options change
- Common issues discovered

### **CHANGELOG.md** (`apps/http-api-app/distribution/CHANGELOG.md`)

Version history and release notes.

**Update this when:**
- New release is prepared
- Features added
- Bugs fixed
- Breaking changes made

### **build.gradle.kts** (`apps/http-api-app/build.gradle.kts`)

Gradle build configuration for the distribution.

**Key settings:**
- `archiveBaseName.set("bisq-trusted-node")` - Package name
- `distributions.main.contents` - Files to include
- `distZip` / `distTar` - Archive formats

---

## 📝 **Version Management**

### **Where Version is Defined**

**Primary source:** `gradle.properties`
```properties
version=2.1.7
```

This version is automatically used in:
- JAR file names (`network-2.1.7.jar`)
- Distribution archives (when properly configured)
- Release notes

### **Updating Version**

1. Edit `gradle.properties`:
   ```properties
   version=2.1.8
   ```

2. Update `apps/http-api-app/distribution/README.md`:
   ```markdown
   **Version:** 2.1.8
   ```

3. Update `apps/http-api-app/distribution/CHANGELOG.md`:
   ```markdown
   ## [2.1.8] - 2025-11-21
   ### Added
   - New feature...
   ```

4. Build: `./build-trusted-node-release.sh`

---

## 🎯 **Key User Instructions (from README)**

### **Password Setup**

Password is optional but strongly recommended specially when sharing your trusted node outside your LAN:
```bash
JAVA_OPTS="-Dapplication.websocket.password=STRONG_PASSWORD" ./bin/http-api-app
```

### **Finding Tor Onion Address**

**Linux/macOS:**
```bash
cat ~/.local/share/bisq2_http_prod/webSocketServer_onionAddress.txt
```

**macOS (latest versions):**
```
cat ~/Library/Application\ Support/bisq2_http_prod/webSocketServer_onionAddress.txt
```

**Windows:**
```cmd
type %USERPROFILE%\.local\share\bisq2_http_prod\webSocketServer_onionAddress.txt
```

### **Instance Name (Profile)**

Each mobile user needs a unique instance:
```bash
JAVA_OPTS="-Dapplication.appName=bisq2_alice" ./bin/http-api-app
```

**Important:** Different `appName` = different profile/data directory

### **Multiple Instances**

Different users need different ports AND appNames:
```bash
# User 1
JAVA_OPTS="-Dapplication.appName=bisq2_alice \
           -Dapplication.websocket.server.port=8090 \
           -Dapplication.websocket.password=alice_pass" \
./bin/http-api-app

# User 2
JAVA_OPTS="-Dapplication.appName=bisq2_bob \
           -Dapplication.websocket.server.port=8091 \
           -Dapplication.websocket.password=bob_pass" \
./bin/http-api-app
```

---

## 🐛 **Common Issues & Solutions**

### **"Tor bootstrap timeout"**

**Cause:** Slow Tor network or restrictive firewall  
**Solution:** Increase timeout in README troubleshooting section

### **"Cannot find Tor binary" (Linux only)**

**Cause:** System Tor not in PATH  
**Solution:** Install system Tor: `sudo apt install tor`

### **"Port already in use"**

**Cause:** Another instance running or port conflict  
**Solution:** Change port with `-Dapplication.websocket.server.port=9000`

### **Large file size (~76 MB)**

**Cause:** Includes all dependencies + embedded Tor  
**Normal:** This is expected and necessary for standalone operation

---

## 📊 **File Sizes**

- **ZIP**: ~76 MB
- **TAR.GZ**: ~76 MB (similar compression)
- **Extracted**: ~80 MB

**Breakdown:**
- Tor binary: ~32 MB
- Bisq2 JARs: ~30 MB
- Dependencies: ~14 MB

---

## 🔐 **Security Notes**

1. **Password is required** - Emphasize in all documentation
2. **Tor provides encryption** - No additional TLS needed
3. **LAN is unencrypted** - Only use on trusted networks
4. **Version compatibility** - Trusted node version must match mobile app

---

## 🚢 **Release Strategy**

### **When to Release**

- **With mobile app releases** - Ensure compatibility
- **Critical bug fixes** - Security or crash fixes
- **Major features** - New functionality for mobile users
- **Performance improvements** - Better user experience

### **Versioning**

Follow Bisq2 versioning:
- **Major.Minor.Patch** (e.g., 2.1.7)
- Match mobile app version for compatibility
- Independent releases allowed for critical fixes

### **Release Channels**

- **Stable**: GitHub releases in bisq-mobile repo
- **Beta**: Pre-release flag on GitHub
- **Dev**: Build from source (not distributed)

---

## 📞 **Support**

If users have issues:
1. Check README troubleshooting section
2. Check logs: `~/.local/share/bisq2_http_prod/bisq_2.log`
3. GitHub issues: https://github.com/bisq-network/bisq-mobile/issues
4. Matrix chat: https://matrix.to/#/#bisq:bitcoin.kyoto

---

## ✅ **Summary**

You now have a complete, production-ready distribution system for Bisq Trusted Node:

1. **Build**: `./build-trusted-node-release.sh`
2. **Test**: Extract and run locally
3. **Release**: Upload to GitHub releases
4. **Support**: Users follow README.md

**The distribution includes:**
- ✅ All dependencies (no build required)
- ✅ Cross-platform scripts
- ✅ Comprehensive documentation
- ✅ Password protection
- ✅ Multi-instance support
- ✅ Tor + Clearnet support

**Users only need:**
- Java 22+
- Extract the archive
- Run the script
- Set a password

---

**Ready to release!** 🚀

