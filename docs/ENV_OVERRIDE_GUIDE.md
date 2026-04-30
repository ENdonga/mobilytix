# Mobilytix — Configuration Override Guide

This guide explains how to override `config.yaml` values without editing
the file directly. This is useful when:

- Your device UDID differs from the team default
- You are running on a physical device instead of an emulator
- You want to run tests on Sauce Labs cloud instead of a local emulator
- A CI pipeline needs to inject device or credential values per job
- You want to test against a different APK version without changing config

---

## How overrides work

Values are resolved in this priority order — highest wins:

```
1. Command line system property    -Dkey=value
2. .env file                       KEY=value
3. config.yaml                     key: "value"
```

The framework checks each source in order and stops at the first match.
`config.yaml` is always the fallback default.

`applyOverrides()` is called once in `BaseTest.globalSetup()` after
`EnvLoader.load()` — this ensures `.env` values are available before
any override resolution runs.

---

## The `.env` file

### What it is

A plain text file at the project root named `.env`. It holds local
machine-specific overrides that you never want to commit to Git.

`.env` is in `.gitignore` — it will never be accidentally committed.
`.env.example` is committed and documents every available override key.

### How to set it up

```bash
# Copy the example file
cp .env.example .env

# Open it and fill in your values
open .env
```

### Format

```
KEY=value
ANOTHER_KEY=another value
```

- One key per line
- No spaces around `=`
- Lines starting with `#` are comments
- Empty lines are ignored

---

## Complete override key reference

### Execution mode

| Key | config.yaml equivalent | Values | Default |
|---|---|---|---|
| `EXECUTION_MODE` | `framework.execution_mode` | `local` \| `sauce_labs` | `local` |

```bash
# Run tests on Sauce Labs cloud instead of local emulator
EXECUTION_MODE=sauce_labs
```

### Device

| Key | config.yaml equivalent | Example |
|---|---|---|
| `DEVICE_UDID` | `device.udid` | `DEVICE_UDID=emulator-5558` |
| `DEVICE_PLATFORM_VERSION` | `device.platform_version` | `DEVICE_PLATFORM_VERSION=13` |
| `DEVICE_NO_RESET` | `device.no_reset` | `DEVICE_NO_RESET=true` |
| `DEVICE_FULL_RESET` | `device.full_reset` | `DEVICE_FULL_RESET=true` |

### Appium server

| Key | config.yaml equivalent | Example |
|---|---|---|
| `APPIUM_HOST` | `framework.appium.host` | `APPIUM_HOST=192.168.1.100` |
| `APPIUM_PORT` | `framework.appium.port` | `APPIUM_PORT=4724` |
| `APPIUM_AUTO_START` | `framework.appium.auto_start` | `APPIUM_AUTO_START=false` |
| `APPIUM_LOG_LEVEL` | `framework.appium.log_level` | `APPIUM_LOG_LEVEL=warn` |

```bash
# Silence Appium HTTP traffic logs in CI — set to warn
APPIUM_LOG_LEVEL=warn

# Disable auto start when Appium is already running externally
APPIUM_AUTO_START=false
```

### Timeouts

| Key | config.yaml equivalent | Example |
|---|---|---|
| `EXPLICIT_TIMEOUT` | `framework.timeouts.explicit` | `EXPLICIT_TIMEOUT=30` |
| `PAGE_LOAD_TIMEOUT` | `framework.timeouts.page_load` | `PAGE_LOAD_TIMEOUT=45` |

```bash
# Increase timeouts for slow CI emulators
EXPLICIT_TIMEOUT=30
PAGE_LOAD_TIMEOUT=45
```

### Reporting

| Key | config.yaml equivalent | Example |
|---|---|---|
| `EXTENT_OUTPUT_PATH` | `reporting.extent.output_path` | `EXTENT_OUTPUT_PATH=target/my-report.html` |
| `SCREENSHOT_ON_FAILURE` | `reporting.screenshots.on_failure` | `SCREENSHOT_ON_FAILURE=true` |
| `SCREENSHOT_ON_PASS` | `reporting.screenshots.on_pass` | `SCREENSHOT_ON_PASS=false` |
| `SCREEN_RECORDING_ENABLED` | `reporting.screen_recording.enabled` | `SCREEN_RECORDING_ENABLED=true` |

### App test credentials

These are the credentials used by test cases to log in to the Sauce Labs
Demo App. They are not Sauce Labs account credentials.

| Key | Used in | Example |
|---|---|---|
| `SAUCE_USERNAME` | `LoginTest`, `CatalogTest` | `SAUCE_USERNAME=bod@example.com` |
| `SAUCE_PASSWORD` | `LoginTest`, `CatalogTest` | `SAUCE_PASSWORD=10203040` |
| `SAUCE_LOCKED_USERNAME` | `LoginTest` | `SAUCE_LOCKED_USERNAME=alice@example.com` |

> These should always come from `.env` or CI environment variables —
> never hardcode credentials in test code or config.yaml.

### Sauce Labs cloud credentials

Only needed when `EXECUTION_MODE=sauce_labs`.
Get these from your Sauce Labs account at **Account → User Settings**.

| Key | Description |
|---|---|
| `SAUCE_LABS_USERNAME` | Your Sauce Labs account username |
| `SAUCE_LABS_ACCESS_KEY` | Your Sauce Labs access key |

```bash
# Switch to cloud execution
EXECUTION_MODE=sauce_labs
APPIUM_AUTO_START=false
SAUCE_LABS_USERNAME=your_username
SAUCE_LABS_ACCESS_KEY=your_access_key
```

### APK source overrides

Override which APK is downloaded in CI without changing `config.yaml`.
Useful when testing against an unreleased version or a private repository.

| Key | config.yaml equivalent | Example |
|---|---|---|
| `APK_DOWNLOAD_URL` | `apps.sauce_demo.apk_source.download_url` | Full APK URL |
| `APK_VERSION` | `apps.sauce_demo.apk_source.version` | `APK_VERSION=2.3.0` |

```bash
# Test against a newer version locally without changing config.yaml
APK_DOWNLOAD_URL=https://github.com/saucelabs/my-demo-app-android/releases/download/2.3.0/mda-2.3.0-26.apk
APK_VERSION=2.3.0
```

### Authentication — OTP and SSO

| Key | Used in | Example |
|---|---|---|
| `OTP_API_TOKEN` | `OtpResolver.resolveFromEmail()` | `OTP_API_TOKEN=abc123` |
| `SSO_USERNAME` | `SsoHandler` | `SSO_USERNAME=user@example.com` |
| `SSO_PASSWORD` | `SsoHandler` | `SSO_PASSWORD=secret` |

---

## Command line overrides

Use `-D` flags with `./mvnw` to override values for a single run.
These take the highest priority and override both `.env` and `config.yaml`.

```bash
# Run against a specific emulator
./mvnw clean test -Ddevice.udid=emulator-5558

# Run against a physical device
./mvnw clean test -Ddevice.udid=R58M123ABCD

# Run on Sauce Labs for one run without editing .env
./mvnw clean test -Dexecution_mode=sauce_labs

# Increase timeouts for a slow device
./mvnw clean test -Dexplicit.timeout=30 -Dpage.load.timeout=45

# Combine multiple overrides
./mvnw clean test \
  -Ddevice.udid=emulator-5558 \
  -Dappium.port=4724 \
  -Dtestng.suite=testng-parallel.xml
```

### Run a specific test

```bash
# All tests (default suite)
./mvnw clean test

# Specific test class
./mvnw test -Dtest=LoginTest

# Specific test method
./mvnw test -Dtest="LoginTest#testValidLogin"

# Parallel suite
./mvnw clean test -Dtestng.suite=testng-parallel.xml
```

---

## When to use each source

| Scenario | Recommended approach |
|---|---|
| Daily local development | Set `DEVICE_UDID` in `.env` once and forget it |
| Running against a different device today | Use `-Ddevice.udid=xxx` for that one run |
| Switching to Sauce Labs for a session | Set `EXECUTION_MODE=sauce_labs` in `.env` |
| CI pipeline | Set environment variables in the workflow — no `.env` needed |
| Testing a specific APK version once | Use `APK_DOWNLOAD_URL` in `.env` temporarily |
| Debugging a flaky test | Use `-Dexplicit.timeout=45` for that run |

---

## `.env.example` — complete reference

```bash
# ============================================================
# Mobilytix local configuration overrides
# Copy this file to .env and fill in your values.
# .env is in .gitignore and will never be committed.
# ============================================================

# ---- Execution mode ----------------------------------------
# Switch between local emulator and Sauce Labs cloud execution
# EXECUTION_MODE=local               # local | sauce_labs

# ---- Device ------------------------------------------------
# DEVICE_UDID=emulator-5554
# DEVICE_PLATFORM_VERSION=14
# DEVICE_NO_RESET=false
# DEVICE_FULL_RESET=false

# ---- Appium server -----------------------------------------
# APPIUM_HOST=127.0.0.1
# APPIUM_PORT=4723
# APPIUM_AUTO_START=true
# APPIUM_LOG_LEVEL=warn              # warn for CI, info for local

# ---- Timeouts (seconds) ------------------------------------
# EXPLICIT_TIMEOUT=15
# PAGE_LOAD_TIMEOUT=30

# ---- Reporting ---------------------------------------------
# EXTENT_OUTPUT_PATH=target/extent-reports/mobilytix-report.html
# SCREENSHOT_ON_FAILURE=true
# SCREENSHOT_ON_PASS=false
# SCREEN_RECORDING_ENABLED=false

# ---- Sauce Labs Demo App test credentials ------------------
# These are app login credentials — NOT Sauce Labs account credentials
SAUCE_USERNAME=bod@example.com
SAUCE_PASSWORD=10203040
SAUCE_LOCKED_USERNAME=alice@example.com

# ---- Sauce Labs cloud execution ----------------------------
# From saucelabs.com → Account → User Settings
# Only needed when EXECUTION_MODE=sauce_labs
# SAUCE_LABS_USERNAME=your_sauce_labs_username
# SAUCE_LABS_ACCESS_KEY=your_sauce_labs_access_key

# ---- APK source override -----------------------------------
# Override to test against a different version without changing config.yaml
# APK_DOWNLOAD_URL=https://github.com/your-org/your-app/releases/download/1.0.0/app.apk
# APK_VERSION=1.0.0

# ---- OTP / SSO ---------------------------------------------
# OTP_API_TOKEN=your_token_here
# SSO_USERNAME=user@example.com
# SSO_PASSWORD=your_password_here
```

---

## CI configuration

In CI there is no `.env` file — set environment variables directly in the
workflow. GitHub Actions reads them as system environment variables which
`resolveOverride()` picks up from source 3 in the priority chain.

```yaml
# In .github/workflows/ci.yml
- name: Run tests on Android Emulator
  uses: reactivecircus/android-emulator-runner@v2
  env:
    EXECUTION_MODE: "local"
    APPIUM_AUTO_START: "true"
    DEVICE_UDID: "emulator-5554"
    APPIUM_LOG_LEVEL: "warn"
    EXPLICIT_TIMEOUT: "45"
    PAGE_LOAD_TIMEOUT: "60"
    SAUCE_USERNAME:        ${{ secrets.SAUCE_USERNAME }}
    SAUCE_PASSWORD:        ${{ secrets.SAUCE_PASSWORD }}
    SAUCE_LOCKED_USERNAME: ${{ secrets.SAUCE_LOCKED_USERNAME }}
```

Sensitive values like credentials should always be stored in
**GitHub Secrets** and injected as environment variables — never
hardcoded in the workflow file.

---

## Adding a new override key

When you need to make a new config value overridable follow this pattern:

1. Add the prop key and env key constants to `ConfigLoader`:

```java
private static final String PROP_MY_KEY = "my.key";       // -Dmy.key=value
private static final String ENV_MY_KEY  = "MY_KEY";        // MY_KEY=value in .env
```

2. Add the resolution block to `resolveAllOverrides()`:

```java
String myOverride = resolveOverride(PROP_MY_KEY, ENV_MY_KEY);
if (myOverride != null && !myOverride.equals(currentValue)) {
    log.info("My key overridden: {} → {} (source: {})",
            currentValue, myOverride,
            getOverrideSource(PROP_MY_KEY, ENV_MY_KEY));
    // apply the override to the config model
}
```

3. Add the key to `.env.example` with a comment
4. Add the key to this guide in the relevant table
5. Commit `.env.example` and this guide — never commit `.env`