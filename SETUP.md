# Mobilytix — Environment Setup Guide

This guide walks you through setting up every dependency required to run the Mobilytix Android Automation Framework on macOS from scratch.  
Follow each step in order. Do not skip a verification step — each one confirms the previous install worked before you build on top of it.

---

## Prerequisites

- macOS 12 (Monterey) or later
- A terminal (the default Terminal app or iTerm2 both work)
- An internet connection
- At least 20 GB of free disk space (Android Studio + SDK takes ~8 GB alone)

---

## Step 1 — Install Homebrew

Homebrew is the macOS package manager. Most tools in this guide are installed through it.

Open your terminal and run:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Follow the on-screen prompts. When it completes, run the two `export` commands it prints at the end (they look like
`export PATH="/opt/homebrew/bin:$PATH"`). These add Homebrew to your shell path.

**Verify:**

```bash
brew --version
```

You should see output like `Homebrew 4.x.x`. If you get `command not found`, close and reopen your terminal, then try again.

---

## Step 2 — Install Java 17

Mobilytix requires Java 17 (LTS). We use the Temurin distribution from Adoptium.

```bash
brew install --cask temurin@17
```

Now tell your shell where Java lives. Open your shell config file in a text editor:

```bash
open ~/.zshrc
```

Add these two lines at the bottom:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

Save the file, then reload it:

```bash
source ~/.zshrc
```

**Verify:**

```bash
java -version
```

Expected output (version numbers may vary slightly):

```
openjdk version "17.x.x" ...
```

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

Expected output:

```
Apache Maven 3.9.x ...
Java version: 17.x.x
```

> **Note — IntelliJ Maven Wrapper:**  
> If you open this project in IntelliJ IDEA, you can use the built-in Maven Wrapper (`mvnw`) instead of the system Maven above.  
> The wrapper is included in the project root and pins the exact Maven version the project was built with.  
> In IntelliJ, right-click `pom.xml` → Maven → Reload Project and IntelliJ will use the wrapper automatically.  
> From the terminal, use `./mvnw` instead of `mvn` for any command listed later in this guide.

---

## Step 4 — Install Node.js via NVM

Appium 2.x is a Node.js application. NVM (Node Version Manager) lets you manage Node versions cleanly.

**Install NVM:**

```bash
brew install nvm
```

Follow the instructions printed after install. They will ask you to add a few lines to `~/.zshrc`. Open the file:

```bash
open ~/.zshrc
```

Add these lines (NVM's installer may have already added them — check first):

```bash
export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && \. "/opt/homebrew/opt/nvm/nvm.sh"
[ -s "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm" ] && \. "/opt/homebrew/opt/nvm/etc/bash_completion.d/nvm"
```

Save and reload:

```bash
source ~/.zshrc
```

**Install the latest LTS version of Node:**

```bash
nvm install --lts
nvm use --lts
nvm alias default node
```

**Verify:**

```bash
node -v
npm -v
```

You should see a Node version of `v20.x.x` or later, and npm `v10.x.x` or later.

---

## Step 5 — Install Appium 2.x

Install Appium globally via npm:

```bash
npm install -g appium
npm install -g @appium/doctor
```

**Verify the Appium version:**

```bash
appium -v
appium-doctor --version
```

You should see `2.x.x` for appium and `2.x.x` for appium-doctor. If you see `1.x.x`, you have an old global install — uninstall it first
with `npm uninstall -g appium` and reinstall.

**Install the UIAutomator2 driver** (required for Android automation):

```bash
appium driver install uiautomator2
```

**Verify the driver is installed:**

```bash
appium driver list --installed
```

You should see `uiautomator2` listed as installed.

---

## Step 6 — Install Android Studio and the Android SDK

Android Studio provides the Android SDK, ADB (Android Debug Bridge), the emulator, and other platform tools.

**Download Android Studio:**

Go to [https://developer.android.com/studio](https://developer.android.com/studio) and download the macOS `.dmg` for your chip (Apple
Silicon or Intel).

Open the `.dmg`, drag Android Studio into your Applications folder, then launch it.

**During the setup wizard:**

- Choose **Standard** installation when prompted
- Accept all licence agreements
- Let it download the Android SDK components (this takes a few minutes)

**After setup, install the Command-line Tools:**

1. In Android Studio, go to **Settings → Languages & Frameworks → Android SDK**
2. Click the **SDK Tools** tab
3. Check **Android SDK Command-line Tools (latest)**
4. Click **Apply** and let it install

**Add the Android SDK to your shell path.** Open `~/.zshrc`:

```bash
open ~/.zshrc
```

Add these lines at the bottom:

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

**Verify ADB is available:**

```bash
adb version
```

Expected output:

```
Android Debug Bridge version 1.0.xx
```

---

## Step 7 — Create an Android Virtual Device (Emulator)

You need an emulator to run tests if you do not have a physical Android device.

1. In Android Studio, go to **Tools → Device Manager**
2. Click **Create Virtual Device**
3. Choose a device definition — **Pixel 7** is a good default
4. Click **Next**, then select a system image — download **API 34 (Android 14)** if not already downloaded
5. Click **Next**, name the AVD, then click **Finish**

**Start the emulator from the terminal:**

```bash
# Add this line in your path
alias pixel7="emulator -avd [Pixel_7_API_34] &"
# use this to start emulator from terminal
emulator -avd Pixel_7_API_34 or
# using the alias to start emulator
pixel7
```

To see available emulators/AVDs run the following command

```bash
emulator -list-avds
```

Expected outputs

```
Pixel7
Pixel_8
```

> Replace `Pixel_7_API_34` with the exact name shown in your Device Manager. You can list all AVDs with `emulator -list-avds`.

Leave the emulator running in the background for the rest of the setup.

**Verify the emulator is detected by ADB:**

```bash
adb devices
```

Expected output:

```
List of devices attached
emulator-5554   device
```

Note the `emulator-5554` value — you will need this as the `udid` in the framework config later.

---

## Step 8 — Install Allure CLI

Allure generates rich HTML reports from your test results.

```bash
brew install allure
```

**Verify:**

```bash
allure --version
```

Expected output: `2.x.x`

---

## Step 9 — Run Appium Doctor

Appium Doctor checks your full environment and flags anything missing or misconfigured. This is your final verification step.

```bash
appium-doctor --android
```

Read through the output carefully.

- Items marked with a green ✔ are correctly configured
- Items marked with a red ✖ need to be fixed before continuing

## Step 10 — Configure IntelliJ to inherit your shell environment

IntelliJ does not load your shell profile by default, which means environment
variables like `ANDROID_HOME` are not visible to the JVM when running tests
or smoke tests from the IDE.

Fix this once after installing IntelliJ:

1. Go to **IntelliJ IDEA → Settings → Tools → Terminal**
2. Set the shell path to `/bin/zsh -l`
   (the `-l` flag loads your ~/.zshrc on startup)
3. Click **Apply** then restart IntelliJ
4. Open a new terminal tab inside IntelliJ and verify:

```bash
echo $ANDROID_HOME
```

You should see your SDK path. Any run configuration launched after this
will also inherit `ANDROID_HOME` correctly.

Common issues and fixes:

| Issue                           | Fix                                         |
|---------------------------------|---------------------------------------------|
| `ANDROID_HOME` not set          | Re-check Step 6 and reload your shell       |
| `adb` not found                 | Confirm `platform-tools` is in your `PATH`  |
| Java version wrong              | Re-check Step 2 and confirm `JAVA_HOME`     |
| `uiautomator2` driver not found | Re-run `appium driver install uiautomator2` |

Do not proceed to Module 1 until `appium doctor --android` shows no critical errors.

---

## Quick Reference — Version Summary

| Tool                | Minimum Version |
|---------------------|-----------------|
| macOS               | 12 (Monterey)   |
| Java                | 17              |
| Maven               | 3.9.x           |
| Node.js             | 20.x (LTS)      |
| npm                 | 10.x            |
| Appium              | 2.x             |
| UIAutomator2 driver | latest          |
| Android SDK API     | 34              |
| Allure CLI          | 2.x             |

---

## Troubleshooting

**`command not found` after installing a tool**  
Your shell config was not reloaded. Run `source ~/.zshrc` and try again. If that does not help, close and reopen your terminal.

**Android Studio SDK path is different on your machine**  
If you installed Android Studio to a custom location, find your SDK path in Android Studio under **Settings → Languages & Frameworks →
Android SDK → Android SDK Location**, then update `ANDROID_HOME` in `~/.zshrc` accordingly.

**Emulator is slow**  
Enable hardware acceleration: in Android Studio go to **Settings → Tools → Emulator** and enable **Hardware HAXM** (Intel) or confirm *
*Hypervisor Framework** is active (Apple Silicon).

**Appium server fails to start**  
Check if the port is already in use:

```bash
lsof -i :4723
```

If a process is listed, kill it with `kill -9 <PID>` where `<PID>` is the number in the second column, then try again.

---

Once `appium doctor --android` is clean and your emulator shows up in `adb devices`, you are ready.  
Give the go-ahead and we will move to **Module 1 — Project Scaffold**.