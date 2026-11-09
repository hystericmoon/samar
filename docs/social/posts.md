# samar launch posts

Site: https://hystericmoon.github.io/samar/ · Download: https://github.com/hystericmoon/samar/releases/latest
Video: `docs/video/samar-demo-4x5.mp4` (1080x1350, 4:5, best for LinkedIn and Instagram feeds), `docs/video/samar-demo.mp4` (1080x1920, 9:16, for Stories/Reels/Shorts/TikTok).
Video thumbnail: `thumbnail-vertical.png` (1080x1920, headline + phone; use it with the 9:16 video, or as a Story cover).
Images: `cover.png` (1200x630 @2x, LinkedIn / X / Facebook link cards), `square.png` (1080x1080 @2x, Instagram / Threads / carousel first slide).

## LinkedIn

I built a small app for a problem I kept running into: you want to share a photo, but not everyone's face in it.

samar is a free Android app that finds the faces in a photo and blurs them. All on your phone.

What I cared about:
→ Fully on-device. The face detection model is bundled in the app. Photos are never uploaded.
→ No accounts, no analytics, no ads.
→ Location and camera info removed from the saved copy by default.
→ You stay in control: blur every face automatically or pick them yourself, resize any blur, or draw one over a face the detector missed.
→ Five looks: Gaussian, Glass, Smear, Pixelate, Solid.

It's open source (Apache 2.0), built with Kotlin and Jetpack Compose, and installs straight from GitHub for now.

Try it, and tell me what you'd change: https://hystericmoon.github.io/samar/

#Android #Privacy #ProductDesign #OpenSource #JetpackCompose

(Attach `docs/video/samar-demo-4x5.mp4` (native upload, 59 s) or `cover.png`.
With the video, many people prefer the link in the first comment instead of the post body. Videos autoplay muted: the on-screen step titles carry the message without sound.)

## X / Threads / Bluesky (short)

Blur faces before you share. Everything stays on your phone.

samar is a free, open-source Android app: finds faces, blurs them, saves a clean copy. No accounts, no uploads, no tracking.

https://hystericmoon.github.io/samar/

## Instagram / Facebook

Share the moment, not everyone's face. 🫥

samar blurs faces in your photos, fully on your phone. Five looks, no accounts, no uploads. Free for Android (link in bio).

#privacy #android #opensource #appdesign #uiux

## Thread follow-up ideas (optional, one per post)

1. The five effects side by side (`docs/readme/effects.png`).
2. The 1-minute demo video (`docs/video/samar-demo.mp4`).
3. Why "samar": Indonesian for faint, indistinct, hard to make out.
