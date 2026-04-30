# Mobilytix

![CI](https://github.com/ENdonga/mobilytix/actions/workflows/ci.yml/badge.svg)

A reusable Android automation framework built with Java, Appium 2.x or later, Maven, and TestNG.
Designed to be app-agnostic — swap the target app via config with no framework code changes.

---

## Features

- Page Object Model with a decoupled logic layer
- `@AppUnderTest` annotation to switch apps via config — no code changes
- Automatic or manual Appium server management
- ADB wrapper for low-level device commands
- Built-in authentication handling — OTP, SSO, basic, and none
- Dual reporting — Allure and ExtentReports
- Parallel execution via TestNG ThreadLocal driver management
- Cloud execution on Sauce Labs with a single config switch
- Central APK store with versioned download config
- Full CI/CD pipeline via GitHub Actions

---

## Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/ENdonga/mobilytix.git
cd mobilytix

# 2. Copy the environment file and fill in your values
cp .env.example .env

# 3. Start your emulator
emulator -avd Pixel_7_API_34 &

# 4. Run all tests
./mvnw clean test
```

See [SETUP.md](SETUP.md) for full environment setup from scratch.

---

## Project Structure

```
mobilytix/
├── .github/workflows/
│   ├── ci.yml                          # Main CI pipeline — full test suite
│   └── ci-smoke.yml                    # Smoke test — APK download verification
├── apks/
│   └── sauce_demo/                     # Place APKs here — gitignored
├── config/
│   └── config.yaml                     # All app and framework configuration
├── src/
│   ├── main/java/io/mobilytix/
│   │   ├── annotation/                 # @AppUnderTest
│   │   ├── api/                        # Auth handlers — OTP, SSO, basic
│   │   ├── adb/                        # ADB wrapper and screen recorder
│   │   ├── config/                     # ConfigLoader, AppConfig, typed models
│   │   ├── core/                       # DriverManager, SessionContext, SuiteContext
│   │   ├── exceptions/                 # Custom exceptions
│   │   ├── pages/
│   │   │   └── sauce_demo/             # Page objects for Sauce Labs Demo App
│   │   ├── reporting/                  # Allure, Extent, MobilytixListener
│   │   └── utils/                      # WaitUtils, LocatorFactory, EnvLoader
│   └── test/java/io/mobilytix/tests/
│       └── sauce_demo/                 # LoginTest, CatalogTest
├── .env.example                        # All supported override keys — copy to .env
├── testng.xml                          # TestNG suite definition
└── pom.xml
```

See [docs/PROJECT_SCAFFOLD.md](docs/PROJECT_SCAFFOLD.md) for a detailed breakdown.

---

## Configuration

All framework and app configuration lives in `config/config.yaml`. You never need to edit
framework source code to change device, app, or execution settings.

### config.yaml structure

```yaml
framework:
  execution_mode: "local"     # local | sauce_labs
  appium:
    auto_start: true
    host: "127.0.0.1"
    port: 4723
    log_level: "info"
  timeouts:
    explicit: 15
    page_load: 30
  sauce_labs:
    region: "eu-central-1"
    build: "mobilytix"
    device_name: "Android GoogleAPI Emulator"
    platform_version: "12"
    app_storage_filename: "MyDemoAppAndroid.apk"

device:
  udid: "emulator-5554"
  platform_version: "14"
  automation_name: "UiAutomator2"

apps:
  sauce_demo:
    app_name: "Sauce Labs Demo App"
    apk_path: "sauce_demo/MyDemoAppAndroid.apk"
    package_name: "com.saucelabs.mydemoapp.android"
    activity: "com.saucelabs.mydemoapp.android.view.activities.SplashActivity"
    requires_auth: false
    auth_type: "basic"
    apk_source:
      base_url: "https://github.com/saucelabs/my-demo-app-android/releases/download"
      version: "2.2.0"
      build: "25"
      filename: "mda-{version}-{build}.apk"
      download_url: "https://github.com/saucelabs/my-demo-app-android/releases/download/2.2.0/mda-2.2.0-25.apk"
```

### ConfigLoader

`ConfigLoader` is the single point of access for all configuration. It is a singleton that:

1. Loads `config.yaml` on first access
2. Maps raw YAML to typed Lombok models (`FrameworkConfig`, `DeviceConfig`, `AppConfig` etc.)
3. Applies overrides from `.env` file and environment variables after `EnvLoader.load()` runs

```java
// Access config anywhere in the framework
ConfigLoader config = ConfigLoader.getInstance();

// App-specific config
AppConfig appConfig = config.getAppConfig("sauce_demo");
String packageName  = appConfig.getPackageName();

// Framework config
int timeout = config.getExplicitTimeout();
boolean isSauceLabs = config.isRunningOnSauceLabs();

// Credentials — resolved from .env or CI environment
String username = config.getCredential(CredentialKeys.SAUCE_USERNAME, "bod@example.com");
```

Override resolution priority — highest wins:

```
1. Command line    -Dkey=value
2. .env file       KEY=value
3. config.yaml     key: "value"
```

---

## Environment Overrides

Copy `.env.example` to `.env` and uncomment what you need. The `.env` file is gitignored
and never committed.

```bash
cp .env.example .env
```

### Supported override keys

| Key | config.yaml equivalent | Example |
|---|---|---|
| `EXECUTION_MODE` | `framework.execution_mode` | `EXECUTION_MODE=sauce_labs` |
| `DEVICE_UDID` | `device.udid` | `DEVICE_UDID=emulator-5558` |
| `DEVICE_PLATFORM_VERSION` | `device.platform_version` | `DEVICE_PLATFORM_VERSION=13` |
| `APPIUM_HOST` | `framework.appium.host` | `APPIUM_HOST=192.168.1.100` |
| `APPIUM_PORT` | `framework.appium.port` | `APPIUM_PORT=4724` |
| `APPIUM_AUTO_START` | `framework.appium.auto_start` | `APPIUM_AUTO_START=false` |
| `APPIUM_LOG_LEVEL` | `framework.appium.log_level` | `APPIUM_LOG_LEVEL=warn` |
| `EXPLICIT_TIMEOUT` | `framework.timeouts.explicit` | `EXPLICIT_TIMEOUT=30` |
| `PAGE_LOAD_TIMEOUT` | `framework.timeouts.page_load` | `PAGE_LOAD_TIMEOUT=45` |
| `DEVICE_NO_RESET` | `device.no_reset` | `DEVICE_NO_RESET=true` |
| `DEVICE_FULL_RESET` | `device.full_reset` | `DEVICE_FULL_RESET=false` |
| `SCREENSHOT_ON_FAILURE` | `reporting.screenshots.on_failure` | `SCREENSHOT_ON_FAILURE=true` |
| `SCREENSHOT_ON_PASS` | `reporting.screenshots.on_pass` | `SCREENSHOT_ON_PASS=false` |
| `SCREEN_RECORDING_ENABLED` | `reporting.screen_recording.enabled` | `SCREEN_RECORDING_ENABLED=true` |
| `APK_DOWNLOAD_URL` | `apps.sauce_demo.apk_source.download_url` | Full APK URL |
| `APK_VERSION` | `apps.sauce_demo.apk_source.version` | `APK_VERSION=2.3.0` |

### App test credentials

```bash
# Sauce Labs Demo App — test credentials (not Sauce Labs account credentials)
SAUCE_USERNAME=bod@example.com
SAUCE_PASSWORD=10203040
SAUCE_LOCKED_USERNAME=alice@example.com
```

### Sauce Labs cloud credentials

```bash
# Only needed when EXECUTION_MODE=sauce_labs
SAUCE_LABS_USERNAME=your_sauce_labs_username
SAUCE_LABS_ACCESS_KEY=your_sauce_labs_access_key
```

See [docs/ENV_OVERRIDE_GUIDE.md](docs/ENV_OVERRIDE_GUIDE.md) for full documentation.

---

## Running Tests

### Local — emulator

```bash
# Run all tests (uses config.yaml defaults)
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=LoginTest

# Run a specific test method
./mvnw test -Dtest=LoginTest#testValidLogin

# Run with custom device
./mvnw clean test -Ddevice.udid=emulator-5558

# Generate and open Allure report
./mvnw allure:serve
```

### Local — Sauce Labs cloud

```bash
# 1. Set credentials in .env
EXECUTION_MODE=sauce_labs
APPIUM_AUTO_START=false
SAUCE_LABS_USERNAME=your_username
SAUCE_LABS_ACCESS_KEY=your_access_key

# 2. Upload APK to Sauce Labs storage (once per version)
curl -u "$SAUCE_LABS_USERNAME:$SAUCE_LABS_ACCESS_KEY" \
  -X POST "https://api.eu-central-1.saucelabs.com/v1/storage/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "payload=@apks/sauce_demo/MyDemoAppAndroid.apk" \
  -F "name=MyDemoAppAndroid.apk"

# 3. Run tests
./mvnw clean test
```

---

## Sauce Labs Integration

Mobilytix supports running tests on Sauce Labs Real Device Cloud with a single config switch.

### How it works

When `execution_mode: sauce_labs` is set the framework:

1. Skips local Appium server start/stop
2. Skips local ADB pre-flight checks
3. Skips local APK file validation
4. Connects `AndroidDriver` to the Sauce Labs endpoint with embedded credentials
5. Sets `sauce:options` capabilities including build name and device orientation
6. Reports pass/fail status back to the Sauce Labs dashboard

```java
// DriverManager switches URL based on execution_mode
// local      → http://127.0.0.1:4723
// sauce_labs → https://username:key@ondemand.eu-central-1.saucelabs.com/wd/hub
```

### Sauce Labs capability keys

All capability key strings are defined in `SauceLabsCapabilityKeys` — no magic strings:

```java
options.setCapability(SauceLabsCapabilityKeys.SAUCE_OPTIONS, Map.of(
    SauceLabsCapabilityKeys.BUILD,              buildName,
    SauceLabsCapabilityKeys.NAME,               appConfig.getAppName(),
    SauceLabsCapabilityKeys.REGION,             slConfig.getRegion(),
    SauceLabsCapabilityKeys.DEVICE_ORIENTATION, SauceLabsCapabilityKeys.PORTRAIT
));
```

### Sign up

Free trial at [saucelabs.com](https://saucelabs.com) — no credit card required.
Get your credentials from **Account → User Settings**.

---

## CI/CD — GitHub Actions

### Main pipeline (`ci.yml`)

Triggers on push to `main` or `develop` and on pull requests to `main`.

```
Checkout → Java 17 → Maven cache → KVM → Node 20 → Appium
    → yq → Read APK config → APK cache → Download APK (cache miss only)
    → Android Emulator → Run all tests
    → Upload Allure results → Upload Extent report → Upload logs
    → Generate Allure report → Deploy to GitHub Pages (main only)
```

**What is cached:**
- Maven dependencies (`~/.m2/repository`) — keyed on `pom.xml` hash
- APK file — keyed on version + build from `config.yaml`
- pip/yq packages — keyed on OS

**Appium is installed fresh every run** — caching Appium causes driver version
conflicts and is not worth the ~20 seconds saved.

### Smoke test pipeline (`ci-smoke.yml`)

Lightweight pipeline for verifying APK download and emulator connectivity.
Runs one test (`LoginTest#testValidLogin`) — no reporting, fastest possible feedback.

Trigger: push to feature branches (configured per branch).

### GitHub Secrets required

Go to **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Description | Required |
|---|---|---|
| `SAUCE_USERNAME` | Demo app login (`bod@example.com`) | Yes |
| `SAUCE_PASSWORD` | Demo app password (`10203040`) | Yes |
| `SAUCE_LOCKED_USERNAME` | Locked user login (`alice@example.com`) | Yes |
| `SAUCE_LABS_USERNAME` | Sauce Labs account username | Cloud runs only |
| `SAUCE_LABS_ACCESS_KEY` | Sauce Labs access key | Cloud runs only |

### Switching CI to Sauce Labs

In `ci.yml` comment out the emulator runner step and uncomment the Sauce Labs section.
Add `SAUCE_LABS_USERNAME` and `SAUCE_LABS_ACCESS_KEY` to GitHub Secrets.

### CI badge

The badge at the top of this README reflects the latest run on `main`.
It turns red on failure — visible without opening the Actions tab.

### Allure report

After every `main` push the Allure report is deployed to GitHub Pages:

```
https://ENdonga.github.io/mobilytix/
```

---

## Adding a New App

1. Drop the APK into `apks/your_app/`
2. Add the app block to `config/config.yaml`:

```yaml
apps:
  your_app:
    app_name: "Your App Name"
    apk_path: "your_app/YourApp.apk"
    package_name: "com.your.package"
    activity: "com.your.package.MainActivity"
    requires_auth: false
    auth_type: "none"
    otp_source: ""
    apk_source:
      base_url: "https://github.com/your-org/your-app/releases/download"
      version: "1.0.0"
      build: "1"
      filename: "app-{version}.apk"
      download_url: ""
```

3. Annotate your test class:

```java
@AppUnderTest("your_app")
public class YourAppTest extends BaseTest {
    // setUp() and driver init happen automatically
}
```

4. Create page objects under `src/main/java/io/mobilytix/pages/your_app/`
5. Write tests under `src/test/java/io/mobilytix/tests/your_app/`
6. Register the test class in `testng.xml`

No framework code changes required.

---

## Test Applications

### Sauce Labs Demo App (primary)

The main reference app for framework examples and test coverage.
Covers catalog browsing, login/logout, sorting, and scrolling.

**Download:** [github.com/saucelabs/my-demo-app-android/releases](https://github.com/saucelabs/my-demo-app-android/releases)

**Place at:** `apks/sauce_demo/MyDemoAppAndroid.apk`

**Test credentials:**

```
Valid user:   bod@example.com  / 10203040
Locked user:  alice@example.com / 10203040
```

**Test coverage:**

| Class | Tests |
|---|---|
| `LoginTest` | Valid login, shortcut login, empty username/password validation, locked user, logout |
| `CatalogTest` | Sort by name A-Z/Z-A, sort by price asc/desc, scroll to bottom, scroll to top, sort preserved after scroll |

---

## Test Lifecycle

The diagram below shows the TestNG annotation execution order and how the
framework hooks into each lifecycle phase.

![Test lifecycle](img.png)

---

## Rebrand Guide

To rename the framework for your organisation change only these five things:

| What | Where | Example |
|---|---|---|
| `groupId` | `pom.xml` | `io.mobilytix` → `com.acme` |
| Java package | IDE refactor | `io.mobilytix` → `com.acme` |
| `framework.name` | `config.yaml` | `Mobilytix` → `Acme Mobile` |
| `artifactId` | `pom.xml` | `mobilytix` → `acme-mobile` |
| Report title | `ExtentManager.java` | `Mobilytix Report` → `Acme Report` |

In IntelliJ: right-click `io.mobilytix` package → **Refactor → Rename**.
All imports update automatically.

See [docs/PROJECT_SCAFFOLD.md](docs/PROJECT_SCAFFOLD.md) section 1.2 for the full rebrand checklist.

---

## Documentation

| Document | Description |
|---|---|
| [SETUP.md](SETUP.md) | Full environment setup from scratch |
| [docs/PROJECT_SCAFFOLD.md](docs/PROJECT_SCAFFOLD.md) | Folder structure and rebrand guide |
| [docs/ENV_OVERRIDE_GUIDE.md](docs/ENV_OVERRIDE_GUIDE.md) | All config override keys and usage |
