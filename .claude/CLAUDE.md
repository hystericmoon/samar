# samar: working agreement for Claude

samar is an Android app (Kotlin, Jetpack Compose) that blurs faces in photos, fully on-device.
Read `DESIGN_BRIEF.md` (source of truth for look and behaviour) before changing UI, and
`RELEASING.md` before releasing.

## Roles

- The owner (hystericmoon) is the **product owner only**. They describe what they want; they do not
  merge, build, test, tag or release.
- Claude takes every request **all the way to a published release** without asking for
  approval at each step. This is a standing instruction from the owner.
- Ask the owner only for product decisions (what it should do or look like). When there is a real
  choice, show it visually (a page with the options drawn as UI, the recommendation marked) per the
  owner's preferences, then continue once they pick.

## Session model

The owner uses one long-lived session and runs `/clear` between tasks, so this file and the repo
are the only memory. The container can also be recycled while idle.

- **Start of every task:** `cd /home/user/samar` (clone it if missing), `git checkout main`,
  `git pull origin main`, then check for leftovers: open PRs, failed or in-progress Release runs,
  and whether the latest release matches `versionName` on `main`. Finish or report any leftover
  before starting new work.
- **End of every task:** nothing may exist only in this container. Everything is merged to `main`
  (or the PR is open and named in the report), the release is published, docs are current, and
  scratch branches are deleted. Anything learned that a future session needs goes into this file.

## Definition of done for every feature or fix

1. Branch from `main`, implement, keep code style and the design system (`ui/components`, `Glass.kt`,
   `SamarIcons`, theme tokens). Check accessibility: 48dp targets, 3:1 / 4.5:1 contrast,
   TalkBack labels and custom actions.
2. Build: `./gradlew :app:compileDebugKotlin` and `./gradlew :app:assembleRelease` must pass.
3. Keep docs in step with the code in the same PR:
   - `DESIGN_BRIEF.md` for any behaviour, UI or copy change.
   - `README.md` for anything a user would notice (features list, how to use, APK file name).
4. Open a PR, then merge it yourself (REST: `PUT /repos/hystericmoon/samar/pulls/{n}/merge`).
5. Release (see below) unless the owner said not to. One feature can be one release; batch only if
   the owner asks.
6. Report to the owner in plain language: what changed for users, the release link, and anything
   that couldn't be verified (e.g. no device test).

## Releasing (fully automated; never needs the owner)

1. In the feature PR or a follow-up PR: raise `versionCode` by 1 and set `versionName` in
   `app/build.gradle.kts` (minor bump for features, patch for fixes), add
   `release-notes/v<version>.md` (same style as the existing ones, no Verify section), update the
   APK name in `README.md`. Merge.
2. Dry run: `POST /repos/hystericmoon/samar/actions/workflows/release.yml/dispatches` with
   `ref=main`, `inputs[version]=<version>`, `inputs[test_only]=true`. Wait for success.
3. Real run: same call without `test_only`. Wait for success.
4. Verify the published release: download `samar-<version>.apk`, check with
   `apksigner verify --print-certs` that the certificate SHA-256 is
   `a679719d…eb6eed23` (full value in `.github/workflows/release.yml`) and with
   `aapt2 dump badging` that versionName/versionCode match. Confirm it's marked latest.

Never publish an APK built or signed any other way. The signing key exists only on the owner's Mac
and in the `release` environment secrets; a release signed with a different key breaks updates for
every user and cannot be undone.

## Environment notes (cloud sessions)

- Attach the repo with push access, clone to `/home/user/samar`.
- The clone is shallow and only tracks `main`. After pushing a new branch, add a fetch refspec
  (`git config --add remote.origin.fetch '+refs/heads/<b>:refs/remotes/origin/<b>'`, `git fetch`,
  `git branch -u origin/<b>`) or the stop hook reports it as unpushed.
- `gh pr create` fails (GraphQL is blocked); use `gh api repos/hystericmoon/samar/pulls ...`.
  Creating releases directly is blocked too; always go through the Release workflow.
- No Android SDK is preinstalled: download commandlinetools, then
  `sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"` and write
  `sdk.dir=...` to `local.properties` (git-ignored).
- Maven Central rate-limits (HTTP 429): build with `--max-workers=1` and retry.
- If a step is blocked by a permission check (e.g. merging a change to `.github/workflows`),
  tell the owner exactly what is blocked and the one action that unblocks it.
