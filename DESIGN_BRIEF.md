# samar: design brief

The source of truth for how samar should look and behave.

## The app

**samar** is an Android app (Kotlin, Jetpack Compose) that blurs faces in a photo, fully on-device. No server, no uploads, no analytics.

- Tagline: "Blur faces before you share."
- Trust line: "Everything stays on your phone."
- Name: "samar" is Indonesian for faint, indistinct, hard to make out. Always lowercase.
- Face detection: ML Kit Face Detection, with the model bundled in the app (no download from Google Play services).
- App ID: `app.samar`. Runs on Android 8.0 and newer (minSdk 26). Compiled against and targeting Android SDK 37.
- Internet is used only to check GitHub for samar updates (once a day, can be turned off in About) and download them. Photos never leave the phone. ML Kit's logging backend is removed so it can't upload.

## Effects

Five effects, plus one intensity slider (0 to 100%, default 70%) that controls strength.

| Effect | What it looks like |
|---|---|
| Gaussian | Soft blur |
| Glass | Frosted blur with a light white tint (no outline) |
| Smear | A long-exposure "head shake": the head streaks sideways in fine speed lines, with two faint ghost positions, and the streak tails run a little past the head into the background (see docs/readme/smear.png) |
| Pixelate | Blocky mosaic |
| Solid | Flat dark fill |

Notes:

- Smear: use an AGSL shader on Android 13+ and a CPU version with the same recipe on older phones. Show a one-line note when Smear is selected: it is a stylistic effect and hides identity less reliably than Solid or Pixelate.
- Mask each face with a slightly padded ellipse (about 20% larger than the detected box) so hairline and ears are covered.
- Soft edges: every effect fades out over about 15% of the face's size just outside the area, so it blends into hair and skin. Inside the area it stays fully opaque.
- Smear has its own edge: streaks carry only the head's colours (the background is never dragged in), cover the hair a little above the face box, and fade out softly. The face is fully covered except across the outer padding rim, where it eases off so no oval outline shows. Streak length 50–140% and tails 30–55% of the face width, from 0 to 100% intensity; slightly darker than the original.
- Rotate the image by its EXIF orientation before detection, or boxes will be misaligned on portrait photos.
- Use a downscaled copy for the live preview (1600px long edge). Run the full-resolution pass only on export.
- Strength scales with each face's size, so the saved photo looks the same as the preview.
- Solid is always a full, opaque cover. When Solid is selected, the intensity slider is replaced by a Colour row ("Always full"): Charcoal #22252B (default), Black, White, Navy #1F06AC, Neon #E3EF4F. 48dp targets, the chosen one gets an emphasis ring and a check. Saved with the photo in Recent and undoable.
- Smear note text: "Smear is a stylistic effect. Solid or Pixelate hide identity more reliably."

## Screens

### 1. Home

- Top bar: logo mark plus the wordmark "samar" on the left, a light/dark switch on the right.
- Hero: a 196dp circular animation of a soft-3D "clay" person, drawn in code (HeroAnimation, ClayPerson). It loops in about 5 seconds:
  1. The person glances left and right, then holds still.
  2. A face box draws itself and a check badge pops in at its top-right corner.
  3. The blur sweeps across the face.
  4. A banded slit-scan wipe (six bands sliding sideways, with thin accent-tint gaps) brings back the clear face.
  - Each loop uses the next effect: Gaussian, Glass, Smear, Pixelate, Solid. The blurred faces are made once with the app's real effect code.
  - It pauses while the app is in the background. With Android's "Remove animations" setting on, it shows one still frame (detected and blurred).
  - It is decorative and hidden from TalkBack.
- Headline "Blur faces before you share." and the trust line below it, centered.
- "Recent" section: heading, a "See all" button, and a 3-column photo grid with 4dp gaps and 6dp corner radius. It shows the latest six. Tapping a photo reopens it in the editor.
  - Empty state: an outlined box that says "Photos you save will show up here." "See all" is hidden while the list is empty.
- A pill "Choose photo" button docked at the bottom.

### 2. Editor

- Top bar: back, title "Edit photo", undo.
- Photo preview. Each detected face has a box and a toggle (check = blurred, plus = not blurred). Tap to switch.
- An eye button on the photo shows the original when tapped (tap to toggle, not press-and-hold). It is a dark circle with a faint light ring, so it shows on any background.
- Zoom: pinch or double-tap (up to 3x), and drag to pan while zoomed. Face boxes follow the zoom, but toggles stay full size (48dp targets). Zoom is kept while editing and resets only for a new photo.
- Tapping the face box opens it in the placement screen ("Adjust blur area", button "Update blur") to make that blur bigger or smaller, move it, or switch Ellipse/Rectangle. Updating also turns its blur on. While adjusting, the photo behind the shape is shown without that face's old blur. TalkBack: the toggle has an "Adjust blur area" action.
- A Preview button (corner-brackets icon, glass, next to the eye) hides all boxes and toggles to show the clean result; tap again to bring them back.
- Face boxes use 14dp corners, at most a quarter of the box size, so small faces still read as boxes.
- Bottom sheet (28dp top corners):
  - Row: a two-line face count on the left and an "Add missed face" glass chip on the right.
    - Strong line "3 faces" (16, bold), quiet line under it (13, muted): "2 blurred · 1 visible", "All blurred", or "None blurred". With no faces: "No faces found" / "Use Add missed face to blur one."
    - TalkBack reads one announcement: "3 faces found, 2 blurred".
  - "Select faces" with an Auto / Manual switch (48dp tall, remembered). Auto: every face found is blurred. Manual: faces are found but none blurred; the quiet line reads "Tap ＋ on a face to blur it". Switching sets all detected faces at once (undoable); added areas are never changed.
  - Horizontally scrolling row of circular effect swatches with labels. Each swatch is the soft-3D clay person from the Home hero, with the real effect applied to its face and a glass-marble sheen. The selected swatch has an accent ring; when chosen it pops up slightly (spring, about 6%) and the person glances left and right once (about 0.6 s). No motion with "Remove animations" on. The row scrolls, so the last swatch is partly visible as a cue.
  - Intensity slider with a percentage.
  - Pill "Export" button.

### 3. Add missed face

- The photo with a draggable, resizable ellipse (or rectangle) the user places over a face the detector missed.
- Four resize handles. The visible handle is small, but each needs a 48dp touch target.
- Hint: "Drag to move, pinch to resize".
- Segmented control: Ellipse / Rectangle.
- Buttons: Cancel, Add blur. The new area uses the current effect and intensity.
- The shape starts where the user was looking in the editor, at the same zoom, and shows the current effect live inside it.
- Gestures depend on where the first finger lands:
  - On the shape: drag moves it and pinch resizes it.
  - On a handle: stretches that one side. A handle wins only when the finger is closer to it than to the shape's centre.
  - Anywhere else: pans and zooms the photo.
- TalkBack: the shape is one element ("Blur area, ellipse") with actions Move up/down/left/right, Make larger, Make smaller.
- Added areas count as faces in "9 faces, 9 blurred" and can be undone.

### 4. Export

- Result preview with a Before / After toggle.
- Two switches:
  - "Remove location and camera info" (on by default). Strip EXIF and GPS from the saved copy.
  - "Keep original resolution" (on by default).
- Buttons: "Save to gallery" (primary), "Share" (outlined).
- Top bar: back (to the editor) on the left, "Done" on the right. Done goes to Home.
  - If the current edits were never saved or shared, Done first asks: "Leave without saving? Your edits to this photo will be lost." (Leave / Keep editing).
- Saved photos go to Pictures/samar as JPEG (quality 92). Android 8–9 ask for storage permission the first time. Newer versions need none.
- Share passes a private temporary copy to the share sheet. Nothing goes over the network.
- "Keep original resolution" on: up to 8192px long edge. Off: 2048px.
- Android's JPEG encoder writes no metadata, so stripping is automatic. With the switch off, date and camera tags are copied back. Location usually can't be, because the photo picker hides it from apps.

## Recent photos

When the user saves a photo, keep a small local list (thumbnail plus effect used) for the Recent grid. Store it only on the device. Provide a way to delete items.

- Up to 30 entries. Each stores a 480px thumbnail of the blurred result (never the original) and the edits: blur areas relative to photo size, effect and intensity. Saving the same photo again replaces its entry.
- Reopening opens the original photo with the edits restored, without re-detecting. samar keeps read access to originals that are in Recent and gives it back when they're removed. If the original is gone, the editor says so.
- "See all" opens a full Recent screen with an "Edit" button. In edit mode each photo gets a × button, plus "Clear all" with a confirmation ("Clear Recent?").
- Removing from Recent never deletes the saved copy in the gallery, and the screen says so.

## Theme (light and dark)

- Follow the system setting by default.
- The Home switch overrides it, and the choice is remembered.
- Photos are never recolored by the theme.

| Token | Light | Dark |
|---|---|---|
| background | #F2F2F6 | #121118 |
| surface | #FFFFFF | #1C1B25 |
| text | #15141C | #F3F2F8 |
| muted text | #55536A | #A9A6BF |
| outline | #8C89A3 | #6F6C86 |
| divider | #DEDCE8 | #2C2A38 |
| accent | #1F06AC (Navy) | #5882E8 (Cornflower) |
| accent tint | #E4E1F7 | #1E2550 |
| text on accent | #FFFFFF | #0B0A24 |

Palette (from the user's reference): Navy #1F06AC, Royal #0F42C0, Cornflower #5882E8, Neon #E3EF4F.

Photo overlays (face boxes, face toggles, the blur-area shape and its handles, the Home hero's box and badge) use Neon #E3EF4F with Navy marks and a dark halo, in both themes. Neon on a white photo is only 1.26:1, so the halo is what keeps 3:1.

Photo placeholder colors used in the design (for previews and empty states only): #C9D6D3, #BFD0CE.

## Type

- **Bricolage Grotesque**: headings, wordmark. Weights 600 to 800, tight letter spacing (about -2% to -3% on large sizes).
- **Instrument Sans**: all interface text. Weights 500 to 700.
- Both are Google Fonts, bundled in the app (static TTFs, about 390 KB), so they work offline and need no network.
- Both are under the SIL Open Font License, which must ship with the app (release prep).

Sizes used: wordmark 26, hero headline 34, screen titles 19, section headings 18, body 14 to 15, labels 13, buttons 16 to 17.

## Glass look (2026-10-20, from design/glass-concept*.png)

- Backgrounds: a vertical fade with a soft cornflower glow at the top-left (`samarBackdrop`). Dark: #0E1240 → #090B2A. Light: #F4F6FD → #E9EDF9.
- Panels (`glassCard`): frosted glass (white 6% in dark, 72% in light), soft top rim, 1dp hairline edge. Sheets (`glassSheet`) are translucent navy (dark) / milky white (light).
- Main buttons: cornflower → navy gradient glass, white text, soft glow.
- `emphasis` colour for numbers and the selected swatch: Neon #E3EF4F in dark, Navy #1F06AC in light.
- Home: logo chip (Neon mark on a Navy disc + "samar") on the left; round glass Theme (sun/moon) and About (ⓘ) buttons on the right. Hero animation 150dp, headline left-aligned, a "Start with" card that follows the editor's Auto / Manual choice (the switch itself lives only in the editor), Choose photo, then Recent: the latest save as a neon card ("3 faces hidden"), then up to three thumbnails.
- Editor: title in a glass pill with the file name above it; effect swatches are 56dp rounded squares (18dp corners) with a glowing ring.
- Export: Before / After at the top, settings in a glass card, and a bottom dock with "N faces hidden", Save and Share.

## Shape

- Pill buttons (fully rounded).
- Liquid glass on every button and switch (Glass.kt, `liquidGlass`), drawn in code so it looks the same on Android 8 and 16 (no background blur):
  - Accent glass (main actions: Choose photo, Export, Save to gallery, Add blur, selected segment, theme knob): translucent accent body, light falling in from the top edge, light bouncing back along the bottom edge, a bright inner rim and a soft shadow that falls below.
  - Clear glass (Share, Cancel, Clear all, top-bar Back/Undo/Close circles, segmented and switch tracks, slider handle, Add missed face chip): frosted white body with a 1dp outline edge kept at 3:1.
  - Over photos: smoky glass for the eye button, frosted glass for the "not blurred" face toggle, accent glass for "blurred". Solid edges keep them at 3:1 on any photo.
  - Pressed: squishes about 3.5% with a spring and brightens.
  - Switches (theme switch, Export switches): the knob stretches like a droplet while it slides, then springs back.
- 28dp top corners on bottom sheets.
- Circular effect swatches (56dp circle in a 72dp slot, selected ring 3dp surface gap plus 3dp accent).
- 6dp corners on photo thumbnails.

## Logo

Six rounded horizontal bars forming a circle, each shifted sideways. Build it as a vector drawable and as an adaptive app icon.

On a 64 x 64 grid, every bar has height 7.5 and corner radius 3.75:

| Bar | x | y | width |
|---|---|---|---|
| 1 | 15.75 | 6.5 | 28.5 |
| 2 | 11.51 | 15.2 | 44.98 |
| 3 | 11.37 | 23.9 | 51.26 |
| 4 | 9.37 | 32.6 | 51.26 |
| 5 | 7.51 | 41.3 | 44.98 |
| 6 | 13.75 | 50.0 | 28.5 |

- App icon: Neon #E3EF4F mark on Navy #1F06AC.
- Themed (monochrome) icon: the mark in a single color.
- Dark variant: same as light (Neon on Navy).
- In the Home top bar, the mark uses the accent color next to the wordmark.

## Accessibility

- 48dp minimum touch targets, including face toggles and resize handles.
- A label on every control. Icon-only buttons get content descriptions (Back, Undo, Show original photo, Close).
- TalkBack announcements for results, for example "3 faces found, 2 blurred". Give the slider a spoken value.
- State is never shown by color alone (the face toggles use check vs plus, solid vs dashed outline).
- Text contrast at least 4.5:1 in both themes. Non-text outlines at least 3:1.
- Make sure the scrolling effect row is reachable and readable with TalkBack.

## Privacy

- Everything runs on the device.
- Strip EXIF and GPS from exported photos by default.
- Do not add analytics or network calls without asking me. (Approved 2026-10-20: the GitHub update check.)

## Updates

- "About samar" (link at the bottom of Home): version, "Check for updates automatically" switch (on by default), Check for updates button with a status line, then the open-source notices.
- Automatic check at most once a day on launch. When a newer release is found, Home shows a glass "Update available" banner above Recent with an Update button and download progress.
- The APK is verified (package app.samar, higher versionCode, same signing certificate) before Android's installer opens. Android 8+ first asks the user to allow samar to install apps.

## Build order

Stop after each step so I can run it. Steps 1–7 are done and tested on a Galaxy A55 (Android 16).

1. Theme (light and dark), fonts, and the logo and app icon.
2. Home with the theme switch (Recent can be empty at first).
3. Editor with face detection and Gaussian only.
4. The other four effects, including the Smear shader.
5. Add missed face.
6. Export, with EXIF stripping.
7. Recent photos.
8. Release prep: license notices, release build and signing, older-Android and TalkBack checks.

Added along the way: editor zoom, the Done button on Export, and the animated soft-3D hero.
