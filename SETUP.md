# Mobilytix — Environment Setup Guide

This guide walks you through setting up every dependency required to run
the Mobilytix Android Automation Framework on macOS from scratch.

Follow each step in order. Do not skip a verification step — each one
confirms the previous install worked before you build on top of it.

---

## Prerequisites

- macOS 12 (Monterey) or later
- A terminal (the default Terminal app or iTerm2 both work)
- An internet connection
- At least 20 GB of free disk space (Android Studio + SDK takes ~8 GB alone)

---

## Step 1 — Install Homebrew

Homebrew is the macOS package manager. Most tools in this guide are
installed through it.

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Follow the on-screen prompts. When it completes run the two `export`
commands it prints at the end — they add Homebrew to your shell path.

**Verify:**

```bash
brew --version
```

Expected: `Homebrew 4.x.x`. If you get `command not found` close and
reopen your terminal then try again.

---

## Step 2 — Install Java 17

Mobilytix requires Java 17 (LTS). We use the Temurin distribution.

```bash
brew install --cask temurin@17
```

Add Java to your shell path:

```bash
open ~/.zshrc
```

Add at the bottom:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

Save and reload:

```bash
source ~/.zshrc
```

**Verify:**

```bash
java -version
```

Expected: `openjdk version "17.x.x" ...`

---

## Step 3 — Install Maven

Maven manages the project's dependencies and build lifecycle.

```bash
brew install maven
```

**Verify:**

```bash
mvn -version
```

Expected:

```
Apache Maven 3.9.x ...
Java version: 17.x.x
```

> **IntelliJ Maven Wrapper note:**
> The project includes `mvnw` — a Maven wrapper that pins the exact
> Maven version the project was built with. In IntelliJ right-click
> `pom.xml` → Maven → Reload Project and IntelliJ uses the wrapper
> automatically. From the terminal use `./mvnw` instead of `mvn`.

---

## Step 4 — Install Node.js via NVM

Appium is a Node.js application. NVM lets you manage Node versions cleanly.

```bash
brew install nvm
```

Add NVM to your shell — open `~/.zshrc` and add these lines if not
already present:

```bash
export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && \. "/opt/homebrew/opt/nvm/nvm.sh"
[ -s "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm" ] && \. "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm"
```

Save and reload:

```bash
source ~/.zshrc
```

Install Node 20 LTS (minimum required by Appium 3.x):

```bash
nvm install 20
nvm use 20
nvm alias default 20
```

**Verify:**

```bash
node -v    # v20.x.x or later
npm -v     # v10.x.x or later
```

---

## Step 5 — Install Appium

Install Appium and the diagnostic tool globally:

```bash
npm install -g appium
npm install -g @appium/doctor
```

**Verify:**

```bash
appium -v
appium-doctor --version
```

Expected: `2.x.x` or later for both. If you see `1.x.x` uninstall the
old version first:

```bash
npm uninstall -g appium
npm install -g appium
```

**Install the UiAutomator2 driver** (required for Android automation):

```bash
appium driver install uiautomator2
```

**Verify:**

```bash
appium driver list --installed
```

Expected: `uiautomator2` listed as installed.

---

## Step 6 — Install Android Studio and the Android SDK

Android Studio provides the Android SDK, ADB, the emulator, and platform tools.

**Download:** [developer.android.com/studio](https://developer.android.com/studio)

Download the macOS `.dmg` for your chip (Apple Silicon or Intel).
Open the `.dmg`, drag Android Studio to Applications, launch it.

**During the setup wizard:**
- Choose **Standard** installation
- Accept all licence agreements
- Let it download SDK components

**After setup — install Command-line Tools:**

1. Go to **Settings → Languages & Frameworks → Android SDK**
2. Click the **SDK Tools** tab
3. Check **Android SDK Command-line Tools (latest)**
4. Click **Apply**

**Add Android SDK to your shell path:**

```bash
open ~/.zshrc
```

Add at the bottom:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/emulator:$PATH
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

Save and reload:

```bash
source ~/.zshrc
```

**Verify:**

```bash
adb version
```

Expected: `Android Debug Bridge version 1.0.xx`

---

## Step 7 — Create an Android Virtual Device

1. In Android Studio go to **Tools → Device Manager**
2. Click **Create Virtual Device**
3. Choose **Pixel 7** as the device definition
4. Click **Next** → select **API 34 (Android 14)** system image
   (download it if not already downloaded)
5. Click **Next** → name the AVD → **Finish**

**Start the emulator from the terminal:**

```bash
# Start in the background
emulator -avd Pixel_7_API_34 &
```

List your available AVDs:

```bash
emulator -list-avds
```

**Create a shell alias for convenience:**

```bash
open ~/.zshrc
```

Add at the bottom (replace `Pixel_7_API_34` with your AVD name):

```bash
alias pixel7="emulator -avd Pixel_7_API_34 &"
```

Reload and use:

```bash
source ~/.zshrc
pixel7           # starts the emulator from any terminal
```

**Verify the emulator is detected:**

```bash
adb devices
```

Expected:

```
List of devices attached
emulator-5554   device
```

Note `emulator-5554` — this is the `udid` value used in `config.yaml`
and your `.env` file.

---

## Step 8 — Install Appium Inspector

Appium Inspector is a GUI tool for finding element locators. You use it
when writing page objects to discover `resource-id`, `content-desc`,
and `class` values.

**Download:** [github.com/appium/appium-inspector/releases](https://github.com/appium/appium-inspector/releases)

Download the latest `.dmg` for macOS, drag to Applications, launch it.

The app opens with a **Server** section and **Desired Capabilities** section.
You do not need to connect it now — you will use it when writing page objects.

**How to use it:**

1. Start your emulator
2. Start Appium server: `appium` in a terminal
3. Open Appium Inspector → connect with your app's capabilities
4. Tap any element to see its locator attributes
5. Use those attributes as `By` locators in your page objects

> Appium Inspector is a development tool — it does not need to run
> during test execution.

---

## Step 9 — Install Allure CLI

Allure generates rich HTML reports from your test results.

```bash
brew install allure
```

**Verify:**

```bash
allure --version
```

Expected: `2.x.x`

**Generate and open a report locally:**

```bash
./mvnw allure:serve
```

---

## Step 10 — Run Appium Doctor

Appium Doctor checks your full environment and flags anything missing.

```bash
appium-doctor --android
```

- ✔ Green — correctly configured
- ✖ Red — needs to be fixed before running tests

Common issues:

| Issue | Fix |
|---|---|
| `ANDROID_HOME` not set | Re-check Step 6 and reload your shell |
| `adb` not found | Confirm `platform-tools` is in your `PATH` |
| Java version wrong | Re-check Step 2 and confirm `JAVA_HOME` |
| `uiautomator2` driver not found | Re-run `appium driver install uiautomator2` |

Do not proceed until `appium-doctor --android` shows no critical errors.

---

## Step 11 — Configure IntelliJ to inherit your shell environment

IntelliJ does not load your shell profile by default. This means
`ANDROID_HOME` and other environment variables are not visible when
running tests from the IDE.

Fix this once:

1. Go to **IntelliJ IDEA → Settings → Tools → Terminal**
2. Set the shell path to `/bin/zsh -l`
   (the `-l` flag loads `~/.zshrc` on startup)
3. Click **Apply** then restart IntelliJ
4. Open a new terminal tab inside IntelliJ and verify:

```bash
echo $ANDROID_HOME
```

You should see your SDK path. Run configurations launched after this
will also inherit environment variables correctly.

---

## Step 12 — Clone and configure the project

```bash
# Clone
git clone https://github.com/ENdonga/mobilytix.git
cd mobilytix

# Copy the environment file
cp .env.example .env

# Fill in your values — at minimum set your device UDID
open .env
```

Minimum `.env` for a local run:

```bash
DEVICE_UDID=emulator-5554          # from `adb devices`
SAUCE_USERNAME=bod@example.com
SAUCE_PASSWORD=10203040
SAUCE_LOCKED_USERNAME=alice@example.com
```

Download the Sauce Labs Demo APK and place it at
`apks/sauce_demo/MyDemoAppAndroid.apk`:

Download from here [github.com/saucelabs/my-demo-app-android/releases](https://github.com/saucelabs/my-demo-app-android/releases)

**Run the tests:**

```bash
# Start emulator first
pixel7

# Wait for emulator to boot, then run
./mvnw clean test
```

---

## Step 13 — Sauce Labs cloud setup (optional)

If you want to run tests on Sauce Labs cloud devices instead of a local
emulator:

1. Sign up at [saucelabs.com](https://saucelabs.com) — free trial, no
   credit card required
2. Get your credentials from **Account → User Settings**
3. Upload the APK to Sauce Labs storage:

```bash
curl -u "YOUR_USERNAME:YOUR_ACCESS_KEY" \
  -X POST "https://api.eu-central-1.saucelabs.com/v1/storage/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "payload=@apks/sauce_demo/MyDemoAppAndroid.apk" \
  -F "name=MyDemoAppAndroid.apk"
```

4. Add to your `.env`:

```bash
EXECUTION_MODE=sauce_labs
APPIUM_AUTO_START=false
SAUCE_LABS_USERNAME=your_username
SAUCE_LABS_ACCESS_KEY=your_access_key
```

5. Run tests — the framework connects to Sauce Labs automatically:

```bash
./mvnw clean test
```

See [docs/ENV_OVERRIDE_GUIDE.md](docs/ENV_OVERRIDE_GUIDE.md) for all
available configuration options.

---

## Quick Reference — Version Summary

| Tool | Minimum version |
|---|---|
| macOS | 12 (Monterey) |
| Java | 17 (Temurin) |
| Maven | 3.9.x |
| Node.js | 20.x LTS |
| npm | 10.x |
| Appium | 2.x |
| UiAutomator2 driver | latest |
| Appium Inspector | latest |
| Android SDK API | 34 |
| Allure CLI | 2.x |

---

## Troubleshooting

**`command not found` after installing a tool**
Run `source ~/.zshrc` and try again. If that does not help close and
reopen your terminal.

**Android Studio SDK path is different**
Find your SDK path in Android Studio under **Settings → Languages &
Frameworks → Android SDK → Android SDK Location** and update
`ANDROID_HOME` in `~/.zshrc`.

**Emulator is slow**
Enable hardware acceleration: **Settings → Tools → Emulator** →
enable **Hardware HAXM** (Intel) or confirm **Hypervisor Framework**
is active (Apple Silicon).

**Appium server fails to start**
Check if port 4723 is already in use:

```bash
lsof -i :4723
kill -9 <PID>
```

**Tests fail with `Device not found`**
Confirm the emulator is running and detected:

```bash
adb devices
```

If it shows `offline` wait 10-15 seconds and run again. If the UDID
shown differs from `emulator-5554` update `DEVICE_UDID` in your `.env`.

**Tests fail with `APK not found`**
Confirm the APK exists at the path in `config.yaml`:

```bash
ls apks/sauce_demo/
```

If empty download the APK from the releases page and place it there.