# 05 — Releasing Corn and Sending Updates

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-13 · **Last updated:** 2026-11-13, 16:02 IST

How Corn gets onto people's phones, how updates reach them, and what has to be in place first. Written for the founder in plain language; the decisions are in the decision log (Round 24).

## Signing keys, in one minute

Every Android app is **signed** with a digital key, like a wax seal. Android uses the seal for one rule:

> An installed app can only be updated by an APK carrying **the same seal** and a **higher version number**.

If both are true, installing the new APK replaces the app and **keeps its data** (Corn's Vault, passwords, settings). If the seal differs, Android refuses; the only way forward is to uninstall the old app, which **wipes its data**.

### The debug key

- Android Studio creates it automatically on each developer's computer (`~/.android/debug.keystore`) so builds can be tested without setup.
- Its password is `android` on every machine in the world. It's for testing only.
- Google Play rejects apps signed with it.
- It exists only on this Mac: lose or reset the Mac and that seal is gone.
- Today's `Corn-0.2.0-test.apk` is signed with it. Fine for a few friends testing; anyone who installs it will have to uninstall (and lose their data) to move to the properly signed version.

### The release key (Corn's real seal)

- Created once by the founder with `keytool` (a `.jks` file + a password), so the password never passes through anyone else. **Done 2026-11-13:** `~/corn-release.jks`.
- Its public certificate (safe to share; lets anyone check an APK really comes from Corn): `CN=Corn, O=Corn, C=IN`, SHA-256 fingerprint `e4b31db31542ecbb4d4bd3903b05ae49439816e4ca8dd7cbe5dea2b74d3930ad`. Check an APK with `apksigner verify --print-certs Corn-x.y.z.apk`.
- **Never** goes into the repo (the repo is public). The build reads its password from `~/.gradle/gradle.properties` on the Mac that builds releases.
- **Back it up** (password manager + an encrypted copy outside this Mac). If it's lost, nobody who installed Corn outside the Play Store can ever get an update again.
- At Play Store launch, upload this same key to Google ("use my existing app signing key" in Play App Signing), so GitHub downloads and Play installs share one seal and people can move between them without losing anything.

### Version numbers

Each APK has an internal `versionCode` (a whole number; Corn's is `1` today) and a visible `versionName` (`0.2.0`). Every release must raise `versionCode` (2, 3, 4…), or Android won't accept it as an update.

## Ways to release

| Channel | Who can install | Updates | Cost / effort | Notes |
|---|---|---|---|---|
| **GitHub Releases** (repo is public) | Anyone with the link, no account | Manual, unless they use Obtainium or Corn checks for updates (below) | Free; a release per version with the APK attached | APKs are release assets, never committed to the repo |
| **Google Play** | Everyone on Android | Automatic | One-time $25 developer account; review; privacy policy, Data Safety form, content rating | The main channel for launch. Play apps may not update themselves |
| **Play internal / closed testing** | Invited testers (up to 100 internal) | Automatic via Play | Same Play account | Good for a beta before public launch |
| **F-Droid** (open-source app store) | Everyone using F-Droid | Automatic | Free; F-Droid builds Corn from the public source; review and build setup | Possible because Corn is open source (MPL-2.0); the filter-list downloads and Google Safe Browsing may need explaining |
| **Firebase App Distribution** | Invited testers | Testers get notified | Free | Test builds only |
| **Direct link** (Drive, website) | Anyone with the link | Manual | Free | No version history; easy to lose track |

## How updates reach people

| How they installed | What happens when there's a new version |
|---|---|
| Play Store | Updates install automatically in the background |
| APK from GitHub, nothing else | Nothing tells them. They must notice, download the new APK and tap Install |
| APK from GitHub + **Obtainium** | Obtainium (a free, open-source app) watches Corn's GitHub releases and notifies or updates. Testers add `github.com/ayush5144/corn` once |
| APK from GitHub + **Corn's own update check** (to build) | Corn asks GitHub for the latest release now and then, shows "Update available", downloads the APK and opens Android's installer (Android always asks the user to confirm) |
| F-Droid | Updates through F-Droid automatically |

Two catches for Corn's own update check:
- **Privacy:** the check contacts GitHub, which sees the phone's IP address (no browsing data). Corn promises nothing about browsing leaves the phone, so the check is explained in Settings and can be switched off.
- **Play rules:** apps installed from Google Play must not update themselves, so the check exists only in the GitHub build and is off in the Play build.

## The plan (founder, 2026-11-13)

1. **Now:** the founder creates the release key; the build signs releases with it; every release raises `versionCode`. Key files are git-ignored.
2. **Testers:** each build is a GitHub pre-release (`v0.2.0-test`, …) with the signed APK and install steps; testers use Obtainium to be told about updates.
3. **Before launch:** Corn's own "Update available" check for the GitHub build only (explained, switchable off, off in the Play build).
4. **At launch:** Google Play with Play App Signing using the same key (automatic updates); F-Droid later if wanted.

Tasks: [checklist, Before launch](02-checklist.md).

## Doing a release (only the values are yours)

1. **Create the key, once:** `scripts/create-release-key.sh`. You only choose a password (typed twice); the certificate details are filled in (CN=Corn, O=Corn, C=IN, public). It won't overwrite an existing key or create one inside the repo.
2. **Fill in the password, once:** open `~/.gradle/gradle.properties` (already there, private to you) and replace both `CHANGE_ME` with that password. Don't paste it anywhere else.
3. **Back up** `~/corn-release.jks` and the password (password manager + an encrypted copy off this Mac).
4. **Each release:** `scripts/release.sh 0.2.1`. It raises `versionCode`, runs the tests, builds, checks the APK is signed with Corn's key (and stops if it's the debug key), copies `Corn-0.2.1.apk` to Downloads, then **asks** before committing the version, tagging, pushing and creating the GitHub pre-release.

How it's wired: `android/app/build.gradle.kts` reads `CORN_RELEASE_STORE_FILE`, `CORN_RELEASE_STORE_PASSWORD`, `CORN_RELEASE_KEY_ALIAS`, `CORN_RELEASE_KEY_PASSWORD` from `~/.gradle/gradle.properties` or from environment variables (for automated builds, e.g. GitHub Actions secrets). Without them, release builds are unsigned, as before. A `.env` file isn't used: Gradle reads its own private file, and a `.env` inside the project could be committed to the public repo by mistake.

## Releases so far

| Version | versionCode | Signed with | Where | Notes |
|---|---|---|---|---|
| 0.2.0 (test builds) | 1 | the **debug key** | Founder's phone, emulator | Testing only; can't be updated by signed releases |
| **0.2.1** | 2 | Corn's release key (SHA-256 e4b31db3…) | [GitHub pre-release v0.2.1](https://github.com/ayush5144/corn/releases/tag/v0.2.1) | First signed release (2026-11-13). Moving from 0.2.0 needs one uninstall (data cleared once) |
| **0.2.2** | 4 | Corn's release key | [GitHub pre-release v0.2.2](https://github.com/ayush5144/corn/releases/tag/v0.2.2) | 2026-11-13: Favourites, Corny above a yellow tagline, new address bar, Vault export, Android share icon, corner sizes. Installs over 0.2.1 keeping data. (versionCode 3 was a local-only build, never published) |

Branches: `main` is Corn (releases are tagged from it); the parked Flush rename lives on `flush`.

## Installing an APK on a phone (for testers)

1. Open the release page on the phone (`github.com/ayush5144/corn/releases`) and tap the `.apk`.
2. Android asks to allow installs from the browser ("Install unknown apps"): allow it once.
3. Tap **Install**. To update later, install the newer APK the same way; data is kept as long as it carries the same seal.
