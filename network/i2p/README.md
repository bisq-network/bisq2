# Bisq: Running Seed Node and Desktop App

This guide explains how to build and run the Bisq Seed Node and Desktop App manually. It also outlines how to configure the desktop app to connect to the locally running seed node.

---

## Prerequisites

- JDK 17 or above
- Gradle installed (or use the Gradle wrapper)
- Unix-based environment (macOS/Linux) or Windows
- Internet access for dependency resolution

---

## 1. Build and Run the Seed Node

### Step 1: Build the Seed Node Binary

```bash
./gradlew :apps:seed-node-app:clean :apps:seed-node-app:installDist
```

### Step 2: Run the Seed Node

```bash
apps/seed-node-app/build/install/seed-node-app/bin/seed-node-app
```

### Step 3: Get the Seed Node Address

After startup, the seed node will output its I2P or Tor address, e.g.:

```
SeedNode address: xhvcxhv.....:1234
```

---

## 2. Configure the Desktop App

Create or edit the file `desk.config` in the project root and add the seed node address from the previous step:

```
--seedNodes=xhvcxhv.....:1234
```

You can add multiple seed node addresses separated by commas if needed.

---

## 3. Build and Run the Desktop App

### Step 1: Build the Desktop App Binary

```bash
./gradlew :apps:desktop:desktop-app:clean :apps:desktop:desktop-app:installDist
```

### Step 2: Run the Desktop App

```bash
apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

---

## 4. Running from an IDE (IntelliJ IDEA)

- First, **run the seed node** from the terminal or an IDE run configuration.
- Copy the seed node address printed on the console into `desk.config`.
- Then, **run the Desktop App** from your IDE with the `desk.config` settings loaded.

---

## Notes

- Ensure that the seed node is up and reachable before starting the desktop app.

---

## Troubleshooting

- **No peers found**: Check if the seed node address in `desk.config` is correct and accessible.
- **Port conflicts**: Ensure no other applications are using the ports required by Bisq.
