# Project Scaffold

In this module you will create the Maven project in IntelliJ IDEA, understand the project naming and rebrand approach, and set up the full folder structure. By the end of this module you will have an empty but correctly organised project ready to receive code in Module 2.

---

## 1.1 — Create the Maven Project in IntelliJ IDEA from scratch

1. Open **IntelliJ IDEA**
2. Click **New Project** from the welcome screen (or **File → New → Project** if you already have a project open)
3. In the left panel select **Maven Archetype**
4. Fill in the fields as follows:

| Field | Value                                                                                                                    |
|---|--------------------------------------------------------------------------------------------------------------------------|
| Name | Enter your project name e.g `mobilytix`                                                                                  |
| Location | Choose a folder on your machine e.g. `~/projects/mobilytix`                                                              |
| JDK | Select **17 (Temurin)** from the dropdown — if it does not appear click **Add JDK** and point it to your Java 17 install |
| Archetype | `maven-archetype-quickstart`                                                                                             |
| Version | `1.0`                                                                                                                    |

5. Expand the **Advanced Settings** section at the bottom and fill in:

| Field | Value                          |
|---|--------------------------------|
| GroupId | Your group Id  `io.mobilytix`  |
| ArtifactId | your artifact name `mobilytix` |
| Version | your version `1.0.0`           |

6. Click **Create**

IntelliJ will generate the project and open it. You will see a basic Maven structure with a `pom.xml` and a sample `App.java`. We will replace all of this in the next modules.

---

## 1.2 — Project Naming and Rebrand Guide - Cloning

The project is named **Mobilytix** — it is deliberately app-agnostic and company-agnostic. If you or someone forking this repository wants to rename it to their company's branding, here is every place that needs to change and nothing else:

| What to change | Where | Example |
|---|---|---|
| `groupId` | `pom.xml` line ~6 | `io.mobilytix` → `com.acme` |
| Java package name | IDE refactor (one step, covered below) | `io.mobilytix` → `com.acme` |
| `framework.name` | `config/config.yaml` | `Mobilytix` → `Acme Mobile` |
| `artifactId` and `name` | `pom.xml` lines ~7–8 | `mobilytix` → `acme-mobile` |
| Report title | `ExtentManager.java` | `Mobilytix Test Report` → `Acme Report` |
| GitHub repository name | GitHub repository settings | — |

**How to rename the Java package in IntelliJ (one step):**

1. In the Project panel expand `src/main/java`
2. Right-click the `io.mobilytix` package → **Refactor → Rename**
3. Type the new package name
4. IntelliJ will update every import and class declaration automatically

No logic, no config keys, and no file paths in the framework code contain the string `mobilytix` — only the package declarations and the two config values listed above.

---

## 1.3 — Delete the Generated Boilerplate

IntelliJ's quickstart archetype creates files you do not need. Delete them now so you start clean.

In the Project panel, delete the following:

- `src/main/java/io/mobilytix/App.java`
- `src/test/java/io/mobilytix/AppTest.java`

Leave the package folders (`io/mobilytix`) in place — you will be creating subpackages inside them.

---

## 1.4 — Create the Full Folder Structure

You are going to create every folder the framework needs now. This means IntelliJ will show you the complete structure from the start and you will always know exactly where each file lives.

Right-click each parent folder in the Project panel and use **New → Package** or **New → Directory** as indicated below.

### Under `src/main/java/io/mobilytix/` — create these packages

Right-click `io.mobilytix` → **New → Package** for each:

```
annotation
api
adb
config
core
pages
pages.app_a
pages.app_b
reporting
utils
```

### Under `src/test/java/io/mobilytix/` — create these packages

```
tests
tests.app_a
tests.app_b
```

### Under `src/main/resources/` — create this folder

Right-click `src/main/resources` → **New → Directory**:

```
config
```

### At the project root level — create these directories

Right-click the project root (the top-level `mobilytix` folder) → **New → Directory** for each:

```
apks
apks/app_a
apks/app_b
```

---

## 1.5 — Verify the Final Structure

After completing the steps above your Project panel should look exactly like this:

```
mobilytix/
├── pom.xml
├── apks/
│   ├── app_a/
│   └── app_b/
└── src/
    ├── main/
    │   ├── java/io/mobilytix/
    │   │   ├── annotation/
    │   │   ├── api/
    │   │   ├── adb/
    │   │   ├── config/
    │   │   ├── core/
    │   │   ├── pages/
    │   │   │   ├── app_a/
    │   │   │   └── app_b/
    │   │   ├── reporting/
    │   │   └── utils/
    │   └── resources/
    │       └── config/
    └── test/
        └── java/io/mobilytix/
            └── tests/
                ├── app_a/
                └── app_b/
```

If anything looks different, fix it before moving on. The rest of the modules drop files into these exact locations.

---

## 1.6 — Add a `.gitignore`

At the project root, create a new file called `.gitignore`. Right-click the root folder → **New → File** → name it `.gitignore`.

Paste the following contents:

```
# Maven build output
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### IntelliJ IDEA ###
.idea/modules.xml
.idea/jarRepositories.xml
.idea/compiler.xml
.idea/libraries/
*.iws
*.iml
*.ipr

### Eclipse ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

### Mac OS ###
.DS_Store

# Logs
*.log
logs/

# Environment secrets — never commit this
.env

# Allure results (generated — not source)
allure-results/

# Screen recordings
screen-recordings/
```
