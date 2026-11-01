# Releasing samar

Releases are built, signed and published by the **Release** workflow on GitHub. No computer needed once it's set up.

## Each release

1. On `main`:
   - Raise `versionCode` (by 1) and set `versionName` in `app/build.gradle.kts`.
   - Add `release-notes/v<version>.md` (what's new, install or update). The *Verify* section with the APK and certificate fingerprints is added automatically.
   - Update the APK name in `README.md` (`samar-<version>.apk`).
2. On GitHub (web or the mobile app): **Actions → Release → Run workflow**, branch `main`, enter the version (e.g. `1.4`).
   Tick *draft* if you want to look it over before users are offered it.

The workflow stops without publishing if:

- the version in the code, the version you typed and the notes file don't match,
- the release already exists, or `versionCode` isn't higher than the latest release (the in-app updater wouldn't offer it),
- the APK isn't signed with samar's upload key (certificate `A6:79:71:…:ED:23`), since an APK signed with any other key can't update existing installs.

Published releases are offered to everyone through the in-app updater within a day.

## One-time setup

The workflow needs samar's upload key. On the Mac that has `~/samar-keys`, in this folder:

```bash
bash scripts/setup-release-secrets.sh
```

It needs the GitHub CLI (`brew install gh`, then `gh auth login`). It creates a `release` environment that only `main` can use and stores the keystore and passwords there as encrypted secrets, so workflows on other branches can't read them.

To set it up by hand instead: **Settings → Environments → New environment** named `release`, limit *Deployment branches* to `main`, and add these secrets:

| Secret | Value |
|---|---|
| `SAMAR_KEYSTORE_BASE64` | output of `base64 < <keystore file>` (the file named in `storeFile`) |
| `SAMAR_STORE_PASSWORD` | `storePassword` from `upload.properties` |
| `SAMAR_KEY_ALIAS` | `keyAlias` |
| `SAMAR_KEY_PASSWORD` | `keyPassword` |

Keep `~/samar-keys` backed up somewhere safe as well. If the key is lost, existing installs can never be updated again.
