# Mobilytix — Configuration Override Guide

This guide explains how to override `config.yaml` values without editing
the file directly. This is useful when:

- Your device udid differs from the team default
- You are running on a physical device instead of an emulator
- A CI pipeline needs to inject device or credential values per job
- You want to test against a different app version without changing config

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

---

## The `.env` file

### What it is

A plain text file at the project root named `.env`. It holds local machine-specific overrides that you never want to commit to Git.

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

## Available override keys

### Device

| Key                       | config.yaml equivalent    | Example                      |
|---------------------------|---------------------------|------------------------------|
| `DEVICE_UDID`             | `device.udid`             | `DEVICE_UDID=emulator-5558`  |
| `DEVICE_PLATFORM_VERSION` | `device.platform_version` | `DEVICE_PLATFORM_VERSION=13` |

### Appium server

| Key           | config.yaml equivalent  | Example                     |
|---------------|-------------------------|-----------------------------|
| `APPIUM_HOST` | `framework.appium.host` | `APPIUM_HOST=192.168.1.100` |
| `APPIUM_PORT` | `framework.appium.port` | `APPIUM_PORT=4724`          |

### Authentication

| Key             | Used in                          | Example                         |
|-----------------|----------------------------------|---------------------------------|
| `OTP_API_TOKEN` | `OtpResolver.resolveFromEmail()` | `OTP_API_TOKEN=abc123`          |
| `SSO_USERNAME`  | `SsoHandler`                     | `SSO_USERNAME=user@example.com` |
| `SSO_PASSWORD`  | `SsoHandler`                     | `SSO_PASSWORD=secret`           |

> Authentication credentials should always come from `.env` or CI
> environment variables — never hardcode them in config.yaml or test code.

---

## Command line overrides

Use `-D` flags with `./mvnw` to override values per run.
These take the highest priority and override both `.env` and `config.yaml`.

### Override device udid

```bash
# Run against a specific emulator
./mvnw clean test -Ddevice.udid=emulator-5558

# Run against a physical device
./mvnw clean test -Ddevice.udid=R58M123ABCD
```

### Override Appium server

```bash
# Run against a remote Appium server
./mvnw clean test -Dappium.host=192.168.1.100 -Dappium.port=4724
```

### Combine multiple overrides

```bash
./mvnw clean test \
  -Ddevice.udid=emulator-5558 \
  -Dappium.port=4724 \
  -Dtestng.suite=testng-parallel.xml
```

### Run a specific test suite

```bash
# Sequential (default)
./mvnw clean test

# Parallel
./mvnw clean test -Dtestng.suite=testng-parallel.xml

# Specific test class
./mvnw clean test -Dtest=MainPageTest

# Specific test method
./mvnw clean test -Dtest=MainPageTest#testMainScreenLoads
```

---

## Using `.env` and command line together

They are designed to complement each other:

| Scenario                                 | Recommended approach                                             |
|------------------------------------------|------------------------------------------------------------------|
| Daily local development                  | Set `DEVICE_UDID` in `.env` once and forget it                   |
| Running against a different device today | Use `-Ddevice.udid=xxx` for that one run                         |
| CI pipeline                              | Set environment variables in the CI job config, no `.env` needed |
| Debugging a specific test                | Use `-Dtest=ClassName#methodName`                                |

### Example daily workflow

```bash
# .env set up once on your machine:
# DEVICE_UDID=emulator-5558

# Normal run — uses .env value automatically
./mvnw clean test

# One-off run against a physical device — command line wins over .env
./mvnw clean test -Ddevice.udid=R58M123ABCD

# Back to normal — .env still has emulator-5558, no changes needed
./mvnw clean test
```

---

## `.env.example` — reference file

The `.env.example` file at the project root is the authoritative list of
every supported override key. It is committed to Git and kept up to date.

Copy it to `.env` and uncomment the lines you need:

```bash
# ============================================================
# Mobilytix local configuration overrides
# Copy this file to .env and fill in your values.
# .env is in .gitignore and will never be committed.
# ============================================================

# ---- Device ------------------------------------------------

# Override the device udid from config.yaml.
# Run `adb devices` to find your device serial.
# DEVICE_UDID=emulator-5554

# Override platform version if your device differs from config.
# DEVICE_PLATFORM_VERSION=14

# ---- Appium server -----------------------------------------

# Override Appium host — useful when running against a remote server.
# APPIUM_HOST=127.0.0.1

# Override Appium port — useful when running multiple Appium instances.
# APPIUM_PORT=4723

# ---- Authentication ----------------------------------------

# API token for email-based OTP resolution (e.g. MailSlurp, Mailinator).
# OTP_API_TOKEN=your_token_here

# SSO credentials — never hardcode these in config.yaml or test code.
# SSO_USERNAME=user@example.com
# SSO_PASSWORD=your_password_here
```

---

## Adding a new override key

When you need to make a new config value overridable:

1. Add the key to `.env.example` with a comment explaining what it does
2. Add the resolution logic to `ConfigLoader` following the same pattern
   as `resolveUdidOverride()` — check system property first, then env var
3. Update this guide with the new key in the relevant table
4. Commit `.env.example` and this guide — never commit `.env`

---

## CI configuration (GitHub Actions example)

In CI you do not use a `.env` file — set environment variables directly
in the workflow:

```yaml
- name: Run tests
  env:
    DEVICE_UDID: emulator-5554
    OTP_API_TOKEN: ${{ secrets.OTP_API_TOKEN }}
  run: ./mvnw clean test
```

Or use `-D` flags:

```yaml
- name: Run tests
  run: |
    ./mvnw clean test \
      -Ddevice.udid=emulator-5554 \
      -Dtestng.suite=testng-parallel.xml
```

Secrets like `OTP_API_TOKEN` should always be stored in GitHub Secrets
and injected as environment variables — never hardcoded in the workflow.