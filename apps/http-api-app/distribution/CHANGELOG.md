# Changelog

All notable changes to Bisq Trusted Node will be documented in this file.

## [2.1.7] - 2025-11-20

### Added
- Initial release of Bisq Trusted Node distribution package
- Support for Tor and Clearnet (LAN) connections
- WebSocket authentication with password protection
- Multi-instance support for multiple mobile users
- Comprehensive README with setup instructions
- Systemd service example for Linux

### Features
- Tor onion service for secure remote connections
- Local network WebSocket server for fast LAN connections
- Automatic Tor bootstrap and hidden service publishing
- Profile isolation per instance (appName)
- Configurable ports and passwords via JAVA_OPTS

### Requirements
- Java 22 or higher
- Internet connection for Tor
- 2-4 GB RAM recommended

### Known Issues
- First Tor bootstrap can take 2-5 minutes
- Linux requires system Tor binary in PATH
- Multiple instances require different ports and appNames

### Security
- Password-protected WebSocket connections
- Tor hidden service encryption
- No external dependencies beyond Java runtime

---

## Future Releases

### Planned Features
- Bundled JRE option (no Java installation required)
- Docker image for easy deployment (for raspberry pi - based platforms)
- Auto-update mechanism
- Web-based admin panel
- Performance monitoring dashboard

---

For detailed release notes, see: https://github.com/bisq-network/bisq-mobile/releases

