#!/bin/bash
# Build Bisq Trusted Node distribution packages
# Usage: ./build-trusted-node-release.sh [version]

set -e

# Get version from gradle.properties if not provided
if [ -z "$1" ]; then
    VERSION=$(grep "^version=" gradle.properties | head -n 1 | cut -d'=' -f2 | tr -d ' \t\r\n')
else
    VERSION="$1"
fi

BUILD_DIR="build/distributions"
RELEASE_DIR="releases/trusted-node-v${VERSION}"

echo "=========================================="
echo "Building Bisq Trusted Node v${VERSION}"
echo "=========================================="
echo ""

# Step 1: Clean previous builds
echo "Step 1: Cleaning previous builds..."
./gradlew :apps:http-api-app:clean
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"
echo "✓ Clean complete"
echo ""

# Step 2: Build the distribution packages
echo "Step 2: Building distribution packages..."
./gradlew :apps:http-api-app:distZip :apps:http-api-app:distTar -x test

if [ $? -ne 0 ]; then
    echo "✗ Build failed!"
    exit 1
fi

echo "✓ Build successful"
echo ""

# Step 3: Copy distribution files to release directory
echo "Step 3: Preparing release files..."

ZIP_FILE="apps/http-api-app/${BUILD_DIR}/bisq-trusted-node-${VERSION}.zip"
TAR_FILE="apps/http-api-app/${BUILD_DIR}/bisq-trusted-node-${VERSION}.tar.gz"

if [ -f "$ZIP_FILE" ]; then
    cp "$ZIP_FILE" "$RELEASE_DIR/"
    echo "✓ Copied ZIP: $(basename $ZIP_FILE)"
else
    echo "⚠ ZIP file not found: $ZIP_FILE"
fi

if [ -f "$TAR_FILE" ]; then
    cp "$TAR_FILE" "$RELEASE_DIR/"
    echo "✓ Copied TAR.GZ: $(basename $TAR_FILE)"
else
    echo "⚠ TAR.GZ file not found: $TAR_FILE"
fi

echo ""

# Step 4: Generate checksums
echo "Step 4: Generating checksums..."
cd "$RELEASE_DIR"

if command -v sha256sum &> /dev/null; then
    sha256sum *.zip *.tar.gz > SHA256SUMS.txt 2>/dev/null || true
    echo "✓ SHA256 checksums generated"
elif command -v shasum &> /dev/null; then
    shasum -a 256 *.zip *.tar.gz > SHA256SUMS.txt 2>/dev/null || true
    echo "✓ SHA256 checksums generated (using shasum)"
else
    echo "⚠ sha256sum/shasum not found, skipping checksums"
fi

cd - > /dev/null
echo ""

# Step 5: Create release notes
echo "Step 5: Creating release notes..."
cat > "$RELEASE_DIR/RELEASE_NOTES.md" <<EOF
# Bisq Trusted Node v${VERSION}

**Release Date:** $(date +%Y-%m-%d)

## 📦 Downloads

- **ZIP (All platforms):** \`bisq-trusted-node-${VERSION}.zip\`
- **TAR.GZ (Linux/macOS):** \`bisq-trusted-node-${VERSION}.tar.gz\`

## 📋 Requirements

- Java 22.0.2 or higher (tested with Java 22.0.2.fx-zulu)
- 2-4 GB RAM
- Internet connection (for Tor)

## 🚀 Quick Start

1. Download and extract the archive
2. Edit \`trusted-node.properties\` and set a strong password
3. Run: \`./run-trusted-node.sh\` (Linux/macOS) or \`run-trusted-node.bat\` (Windows)
4. Find your Tor onion address in the terminal output or data directory
5. Connect your Bisq Connect mobile app using \`ws://your-onion.onion:8090\`

## 📖 Configuration

Edit \`trusted-node.properties\`:

\`\`\`properties
appName=bisq2_http_prod
password=YOUR_STRONG_PASSWORD_HERE
port=8090
devMode=false
transportTypes=TOR,CLEAR
\`\`\`

## 📖 Full Documentation

See \`README.md\` in the distribution package for complete setup instructions.

## 🔒 Verify Download

\`\`\`bash
sha256sum -c SHA256SUMS.txt
\`\`\`

## 📞 Support

- [GitHub](https://github.com/bisq-network/bisq-mobile)
- [Matrix](https://matrix.to/#/#bisq:bitcoin.kyoto)
- [Forum](https://bisq.community/)

---

**Compatible with:** Bisq Connect Mobile App v${VERSION}
EOF

echo "✓ Release notes created"
echo ""

# Step 6: List release files
echo "=========================================="
echo "✓ Release Build Complete!"
echo "=========================================="
echo ""
echo "Release files in: $RELEASE_DIR"
echo ""
ls -lh "$RELEASE_DIR"
echo ""

# Step 7: Calculate total size
TOTAL_SIZE=$(du -sh "$RELEASE_DIR" | cut -f1)
echo "Total release size: $TOTAL_SIZE"
echo ""

# Step 8: Show next steps
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo ""
echo "1. Test the distribution:"
echo "   cd $RELEASE_DIR"
echo "   unzip bisq-trusted-node-${VERSION}.zip"
echo "   cd bisq-trusted-node-${VERSION}"
echo "   # Edit trusted-node.properties and set a password"
echo "   ./run-trusted-node.sh"
echo ""
echo "2. Create GitHub release:"
echo "   - Go to: https://github.com/bisq-network/bisq-mobile/releases/new"
echo "   - Tag: v${VERSION}"
echo "   - Title: Bisq Trusted Node v${VERSION}"
echo "   - Upload files from: $RELEASE_DIR"
echo "   - Copy release notes from: $RELEASE_DIR/RELEASE_NOTES.md"
echo ""
echo "3. Verify checksums:"
echo "   cd $RELEASE_DIR"
echo "   sha256sum -c SHA256SUMS.txt"
echo ""
echo "=========================================="
echo "Build completed successfully! 🎉"
echo "=========================================="

