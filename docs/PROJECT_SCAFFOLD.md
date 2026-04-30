# Mobilytix — Project Structure Guide

This document explains the project layout, what lives where and why,
the key architecture decisions, and how CI/CD is configured.
Read this before writing any new code or adding a new app.

---

## Folder Structure

```
mobilytix/
├── .github/
│   └── workflows/
│       ├── ci.yml                     # Main CI — full test suite on every push
│       └── ci-smoke.yml               # Smoke test — APK download verification
├── apks/
│   └── sauce_demo/                    # APK files live here — gitignored
│       └── .gitkeep                   # Keeps the folder in Git without the APK
├── docs/
│   ├── ENV_OVERRIDE_GUIDE.md          # All config override keys and usage
│   └── PROJECT_SCAFFOLD.md            # This file
├── src/
│   ├── main/
│   │   ├── java/io/mobilytix/
│   │   │   ├── annotation/            # @AppUnderTest — declares which app a test targets
│   │   │   ├── api/                   # Auth handlers — OTP, SSO, basic, magic link
│   │   │   ├── adb/                   # ADB wrapper and screen recorder
│   │   │   ├── config/                # All configuration — models, loader, keys
│   │   │   ├── core/                  # Driver management, session state, suite state
│   │   │   ├── exceptions/            # Custom exceptions with actionable messages
│   │   │   ├── pages/                 # Page objects — one subfolder per app
│   │   │   │   └── sauce_demo/        # Sauce Labs Demo App page objects
│   │   │   ├── reporting/             # Allure, Extent, TestNG listener
│   │   │   └── utils/                 # Wait utilities, locator factory, env loader
│   │   └── resources/
│   │       ├── config/
│   │       │   └── config.yml         # All app and framework configuration
│   │       └── log4j2.xml             # Logging configuration
│   └── test/
│       └── java/io/mobilytix/
│           └── tests/
│               ├── BaseTest.java      # All test classes extend this
│               └── sauce_demo/        # Tests for Sauce Labs Demo App
├── .env.example                       # All supported override keys — copy to .env
├── .gitignore
├── pom.xml                            # Dependencies and build config
├── README.md
├── SETUP.md                           # Environment setup from scratch
└── testng.xml                         # TestNG suite definition
```

---

## Package by package

### `annotation`

Contains `@AppUnderTest` — the single annotation that connects a test
class to an app configuration entry in `config.yaml`.

```java
@AppUnderTest("sauce_demo")
public class LoginTest extends BaseTest { }
```

`BaseTest.setUp()` reads this annotation via reflection and loads the
matching `AppConfig`. Adding a new app requires no framework code
changes — only a new config block and page objects.

---

### `api`

Authentication handlers for apps that require login before testing:

| Class | Handles |
|---|---|
| `AuthHandler` | Orchestrates the auth flow based on `auth_type` in config |
| `OtpResolver` | Email and SMS OTP via external API |
| `SsoHandler` | SSO login flows |
| `MagicLinkHandler` | Magic link auth via email |
| `AppSwitcher` | Switching between apps during a session |
| `SessionStateManager` | Tracks and restores session state |

Auth type is declared per app in `config.yaml`:

```yaml
apps:
  sauce_demo:
    requires_auth: false
    auth_type: "basic"    # basic | otp | sso | magic_link | none
    otp_source: ""        # sms | email | totp — only for auth_type: otp
```

---

### `adb`

Direct ADB command wrapper — used for device-level operations that
Appium does not expose:

| Class | Responsibility |
|---|---|
| `AdbCommands` | Shell commands, device state, logcat, app data |
| `ScreenRecorder` | Start/stop MP4 screen recording via `screenrecord` |
| `ApkManager` | Install, uninstall, verify APKs |

`AdbCommands` is a singleton — thread-safe for parallel execution.
ADB path is resolved automatically from `ANDROID_HOME` via `EnvUtils`.

Key methods:

```java
adb.shell("pm", "clear", packageName);         // clear all app data
adb.restartApp(packageName, activity);          // force stop + relaunch
adb.assertDeviceReady();                        // throws if device offline
adb.clearLogcat();                              // clear logcat before each test
```

---

### `config`

The configuration system — single source of truth for everything.

| Class | Responsibility |
|---|---|
| `ConfigLoader` | Singleton — loads `config.yaml`, applies overrides |
| `AppConfig` | Per-app settings — package, activity, auth, APK path |
| `ApkSource` | APK download URL construction for CI |
| `FrameworkConfig` | Appium, timeouts, execution mode, Sauce Labs |
| `AppiumConfig` | Host, port, auto_start, log level |
| `DeviceConfig` | UDID, platform version, reset flags |
| `TimeoutsConfig` | Explicit and page load timeouts |
| `ReportingConfig` | Screenshots, screen recording, Extent path |
| `SauceLabsConfig` | Region, device name, build, app storage filename |
| `SauceLabsCapabilityKeys` | String constants for `sauce:options` keys |
| `CredentialKeys` | String constants for all credential env var keys |

**Override resolution order** — highest wins:

```
1. Command line   -Dkey=value
2. .env file      KEY=value
3. config.yaml    key: value
```

`applyOverrides()` must be called after `EnvLoader.load()` in
`BaseTest.globalSetup()` — this is when `.env` values become available.

**Adding a new override key** — follow this pattern in `ConfigLoader`:

```java
private static final String PROP_MY_KEY = "my.key";
private static final String ENV_MY_KEY  = "MY_KEY";

// In resolveAllOverrides()
String override = resolveOverride(PROP_MY_KEY, ENV_MY_KEY);
if (override != null && !override.equals(currentValue)) {
    log.info("My key overridden: {} → {}", currentValue, override);
    // apply to model
}
```

---

### `core`

The driver and session management layer.

| Class | Responsibility |
|---|---|
| `DriverManager` | ThreadLocal `AndroidDriver` — one per thread for parallel safety |
| `AppiumServerManager` | Start/stop local Appium server, health check |
| `SessionContext` | Per-thread app key and `AppConfig` — cleared after each class |
| `SuiteContext` | Suite-level abort state — shared between `BaseTest` and listener |

**Why `SuiteContext` is separate from `BaseTest`:**
`MobilytixListener` needs to check whether the suite was aborted but
cannot import `BaseTest` (circular dependency — `BaseTest` registers
the listener). `SuiteContext` lives in `core` and is safely importable
by both.

**Why `ThreadLocal` driver:**
Each thread in a parallel run gets its own `AndroidDriver` instance.
`DriverManager` is a singleton that manages the `ThreadLocal` — one
shared manager, isolated driver state per thread.

**Sauce Labs mode:**
When `execution_mode: sauce_labs` the driver connects to the Sauce Labs
endpoint instead of local Appium. `AppiumServerManager` skips start/stop.
Pre-flight checks are skipped. APK file validation is skipped.

```java
// DriverManager switches URL based on execution_mode
// local      → http://127.0.0.1:4723
// sauce_labs → https://user:key@ondemand.eu-central-1.saucelabs.com/wd/hub
```

---

### `exceptions`

Custom exceptions with clear, actionable error messages. Every exception
includes fix instructions so the developer knows exactly what to do:

| Exception | When thrown |
|---|---|
| `DeviceNotReadyException` | ADB device not found or offline |
| `AppiumServerException` | Appium server not running or unreachable |
| `DriverInitException` | AndroidDriver session could not be created |
| `ApkNotFoundException` | APK file not found at configured path |
| `ConfigException` | `config.yaml` missing, malformed, or key not found |
| `AuthenticationException` | Login flow failed |
| `PageNotLoadedException` | Page did not load within timeout |
| `AdbCommandException` | ADB command exited with non-zero code |

---

### `pages`

Page objects — one subfolder per app under `pages/`.

**`BasePage`** is the abstract base class all page objects extend.
It provides:

- `tap()`, `type()`, `typeSecret()`, `find()`, `findAll()` — element interaction
- `isDisplayed()` — safe visibility check, returns false instead of throwing
- `scrollDown()`, `scrollUp()`, `scrollToElement()` — scroll operations
- `waitFor(locator)` — fluent wait builder (`PageWait`)
- `takeScreenshot()` — returns bytes for reporting
- `abstract isLoaded()` — each page defines what loaded means
- `abstract waitForPageLoad()` — each page defines what fully interactive means

**`PageWait`** fluent builder for chaining waits:

```java
// In CatalogPage.waitForPageLoad()
waitFor(PRODUCT_LIST).toBeVisible()
                     .then(MENU_BUTTON).toBeClickable()
                     .done();
```

**`LocatorFactory`** dynamic locator builder:

```java
// Reads package name from SessionContext — no hardcoded package names
LocatorFactory.byId("productRV")                      // app element
LocatorFactory.byId("android", "search_src_text")     // system element
LocatorFactory.byFullId("com.example:id/element")     // fully qualified
LocatorFactory.byAccessibility("View menu")
LocatorFactory.byText("Reset App State")
LocatorFactory.byUiAutomator("new UiSelector()...")
```

**Adding a new app** — create a subfolder under `pages/` and extend `BasePage`:

```java
public class MyAppPage extends BasePage {

    private static final By MY_ELEMENT = LocatorFactory.byId("element_id");
    private static MyAppPage instance;

    private MyAppPage() {}

    public static MyAppPage getInstance() {
        if (instance == null) instance = new MyAppPage();
        return instance;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(MY_ELEMENT);
    }

    @Override
    public void waitForPageLoad() {
        waitFor(MY_ELEMENT).toBeVisible().done();
    }
}
```

---

### `reporting`

| Class | Responsibility |
|---|---|
| `MobilytixListener` | TestNG listener — logs test start/pass/fail, attaches screenshots, checks `SuiteContext` |
| `ExtentManager` | Initialises and flushes the ExtentReports HTML report |
| `AllureAttachments` | Attaches screenshots and steps to Allure results |

`MobilytixListener` is registered in `testng.xml` and checks
`SuiteContext.isAborted()` before doing anything — if the suite aborted
during pre-flight all listener actions are skipped silently.

---

### `utils`

| Class | Responsibility |
|---|---|
| `WaitUtils` | `FluentWait` wrappers — visible, clickable, invisible, text, all visible |
| `LocatorFactory` | Builds `By` locators — reads package from `SessionContext` |
| `PageWait` | Fluent wait builder used inside page objects |
| `EnvLoader` | Reads `.env` file and loads keys into system properties |
| `EnvUtils` | Resolves ADB path from `ANDROID_HOME` |
| `DataUtils` | Sort helpers — alphabetical, reversed, price ascending/descending |

`WaitUtils` uses `FluentWait` with 500ms polling — every wait returns
as soon as the condition is met, not after the full timeout. This is
what makes tests fast on a warmed-up emulator and patient on a slow
CI emulator.

---

## Test layer

### `BaseTest`

All test classes extend `BaseTest`. It owns the TestNG lifecycle hooks:

| Hook | What it does |
|---|---|
| `@BeforeSuite` | `EnvLoader.load()` → `ConfigLoader.applyOverrides()` → start Appium → pre-flight checks |
| `@BeforeClass` | Resolve `@AppUnderTest` → load `AppConfig` → init `AndroidDriver` |
| `@AfterClass` | Quit driver → clear `SessionContext` |
| `@AfterSuite` | Flush reports → stop Appium |

Pre-flight checks are skipped entirely in Sauce Labs mode.
If `@BeforeSuite` fails `SuiteContext.abort()` is called — all
subsequent `@BeforeClass` methods throw `SkipException` immediately,
`MobilytixListener` logs once and skips silently for every test.

### Test classes

Each test class is annotated with `@AppUnderTest` and extends `BaseTest`:

```java
@AppUnderTest("sauce_demo")
@Epic("Sauce Labs Demo App")
@Feature("Login")
public class LoginTest extends BaseTest {

    @BeforeClass(dependsOnMethods = "setUp")
    public void setUpPage() { ... }

    @BeforeMethod
    public void resetToLoginScreen() { ... }

    @Test
    public void testValidLogin() { ... }
}
```

`setUpPage()` uses `dependsOnMethods = "setUp"` — if `setUp()` throws
`SkipException` (suite aborted), `setUpPage()` is skipped automatically.

---

## CI/CD — GitHub Actions

### Main pipeline — `ci.yml`

Triggers on push to `main` or `develop` and on pull requests to `main`.

```
Checkout code
  → Set up Java 17 + cache Maven
  → Enable KVM (emulator hardware acceleration)
  → Set up Node 20
  → Install Appium + UiAutomator2 driver
  → Cache pip + install yq
  → Read APK config from config.yaml (via yq)
  → Cache APK (keyed on version + build)
  → Download APK (cache miss only)
  → Run tests on Android Emulator (reactivecircus/android-emulator-runner)
  → Upload Allure results, Extent report, logs (always)
  → [report job] Generate Allure HTML report
  → [report job] Deploy to GitHub Pages (main push only)
```

**What is cached:**

| Cache | Key | Saves |
|---|---|---|
| Maven deps `~/.m2/repository` | `pom.xml` hash | 2-3 min |
| APK file | version + build from `config.yaml` | 30-60s download |
| pip/yq | OS | ~5s |

Appium is **not cached** — caching Appium causes driver version conflicts
and saves only ~20 seconds which is not worth the instability.

**APK version management:**
The APK download URL and version live in `config.yaml` under
`apps.sauce_demo.apk_source`. The CI workflow reads these via `yq`
and uses them as the cache key. To upgrade the APK bump `version`
and `build` in `config.yaml` — the cache key changes automatically
and CI downloads the new APK.

```yaml
apk_source:
  base_url: "https://github.com/saucelabs/my-demo-app-android/releases/download"
  version: "2.2.0"        # ← bump to invalidate cache and download new APK
  build: "25"
  filename: "mda-{version}-{build}.apk"
  download_url: "https://..."  # full URL — takes priority over constructed URL
```

If `download_url` is blank the CI script constructs the URL from
`base_url`, `version`, `build`, and `filename` — matching exactly what
`ApkSource.resolveDownloadUrl()` does in Java.

**Switching to Sauce Labs in CI:**
Comment out the emulator runner step and uncomment the Sauce Labs
section in `ci.yml`. Add `SAUCE_LABS_USERNAME` and
`SAUCE_LABS_ACCESS_KEY` to GitHub Secrets.

### Smoke test pipeline — `ci-smoke.yml`

Lightweight pipeline for verifying a specific branch without running
the full test suite. Runs only `LoginTest#testValidLogin` — no
reporting, fastest possible feedback.

Use this when:
- Verifying a new branch before merging
- Testing APK download changes without a full run
- Checking emulator connectivity after workflow changes

Trigger: push to a configured branch (set `branches` in the file).
Once verified change the trigger to `workflow_dispatch` so it only
runs manually, then register it on `main` to appear in the Actions UI.

### GitHub Secrets

Go to **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Description | Required |
|---|---|---|
| `SAUCE_USERNAME` | Demo app login — `bod@example.com` | Yes |
| `SAUCE_PASSWORD` | Demo app password — `10203040` | Yes |
| `SAUCE_LOCKED_USERNAME` | Locked user — `alice@example.com` | Yes |
| `SAUCE_LABS_USERNAME` | Sauce Labs account username | Cloud runs only |
| `SAUCE_LABS_ACCESS_KEY` | Sauce Labs access key | Cloud runs only |

### Allure report

After every push to `main` the Allure HTML report is deployed to
GitHub Pages and accessible at:

```
https://ENdonga.github.io/mobilytix/
```

The CI badge in `README.md` reflects the latest `main` run status.

---

## Adding a new app

1. Drop the APK into `apks/your_app/`

2. Add the app block to `config/config.yaml`:

```yaml
apps:
  your_app:
    app_name: "Your App"
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

3. Create page objects under `src/main/java/io/mobilytix/pages/your_app/`

4. Write tests under `src/test/java/io/mobilytix/tests/your_app/`:

```java
@AppUnderTest("your_app")
public class YourAppTest extends BaseTest {
    @BeforeClass(dependsOnMethods = "setUp")
    public void setUpPage() { ... }
}
```

5. Register in `testng.xml`:

```xml
<test name="Your App Tests">
    <classes>
        <class name="io.mobilytix.tests.your_app.YourAppTest"/>
    </classes>
</test>
```

No framework code changes required.

---

## Rebrand guide

To rename the framework for your organisation change only these five things:

| What | Where | Example |
|---|---|---|
| `groupId` | `pom.xml` | `io.mobilytix` → `com.acme` |
| Java package | IDE refactor | `io.mobilytix` → `com.acme` |
| `framework.name` | `config.yaml` | `Mobilytix` → `Acme Mobile` |
| `artifactId` | `pom.xml` | `mobilytix` → `acme-mobile` |
| Report title | `ExtentManager.java` | `Mobilytix Report` → `Acme Report` |

In IntelliJ: right-click `io.mobilytix` → **Refactor → Rename**.
All imports update automatically.

No logic, config keys, or file paths in the framework contain the
string `mobilytix` — only package declarations and the two config
values above.