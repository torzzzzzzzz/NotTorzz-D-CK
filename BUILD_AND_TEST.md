# Build & Test Results

All versions were verified against **live Mojang and Fabric metadata** (not from
memory) before writing any code, and the mod was compiled and run against the real
game.

## Toolchain (verified)

| Component | Version | How verified |
|---|---|---|
| Minecraft | `1.21.11` | Present & stable in Mojang `version_manifest_v2.json` and Fabric `/v2/versions/game` |
| Yarn mappings | `1.21.11+build.6` | Fabric `/v2/versions/yarn/1.21.11` |
| Fabric Loader | `0.20.1` | Fabric `/v2/versions/loader` |
| Fabric API | `0.141.6+1.21.11` | Modrinth API for project `fabric-api` filtered to `1.21.11` |
| Fabric Loom | `1.19.4` | Latest stable on `maven.fabricmc.net` |
| Gradle | `9.9.1` | Required by Loom 1.19.4 (≥9.8); installed via wrapper |
| Java (build) | `25` (Temurin) | Required by Loom 1.19.4's plugin runtime |
| Java (mod target) | `21` | `options.release = 21` for broad runtime compatibility |

Every Minecraft/Yarn API the code calls (the rewritten `DrawContext` +
`Matrix3x2fStack`, the new `Click`/`KeyInput` input records, the refactored
`PositionedSoundInstance.ui(...)`, the new typed `KeyBinding.Category`, the scoreboard
and title APIs) was confirmed against the extracted `1.21.11+build.6` Yarn mappings.

## Compile / package

```
$ ./gradlew build
BUILD SUCCESSFUL
```

Artifacts in `build/libs/`:
* `nottorzz-dck-1.0.0.jar` — the loadable, remapped mod (~37 KB)
* `nottorzz-dck-1.0.0-sources.jar` — sources for IDEs

`fabric.mod.json` validates (id `nottorzz_dck`, `environment: client`, client
entrypoint, correct `depends`).

## Headless in-game load + render test

The mod was launched in the **real Minecraft 1.21.11 client** headlessly
(`./gradlew runClient` under `Xvfb` + Mesa `llvmpipe` software OpenGL, Java 25).

Confirmed from the client log:

```
Loading Minecraft 1.21.11 with Fabric Loader 0.20.1
 - minecraft 1.21.11
 - fabricloader 0.20.1
 - java 25
 - nottorzz_dck 1.0.0
[Render thread/INFO] (NotTorzz D*CK) [NotTorzz] NotTorzz D*CK loaded (cosmetic-only).
```

* The mod **loads and initialises cleanly** in the real client, listed in the
  resource-reload set alongside the full Fabric API — **no crash, no exception**.
* The client reached a fully rendered state and the custom GUI rendered correctly on
  the real Minecraft font + render pipeline.
* Six real framebuffer screenshots were captured (one per visual state) using
  Minecraft's own `ScreenshotRecorder` — they are in [`docs/images/`](docs/images):
  `00_title`, `01_menu`, `02_reveal_dontpay`, `03_reveal_pay`, `04_safe`,
  `05_countdown`.

> The screenshots were produced by a tiny dev-only harness (`DevPreview.java`) that is
> **gated behind the `NOTTORZZ_DEVPREVIEW=1` environment variable** and is a complete
> no-op in a normal install.

## Test matrix (vs. the brief)

| Test | Result | Notes |
|---|---|---|
| DonutSMP join detection | ✅ logic verified | Matches `ServerInfo.address` against `serverMatch`. A live connection to DonutSMP needs online auth, which isn't available in this headless CI, so the *trigger* was validated by code + the activation path; everything it fires was validated in-client. |
| Balance display when available | ✅ | Read-only chat + sidebar scanning with a tolerant parser; demo path rendered a real balance in-client. |
| Correct 90% calculation | ✅ | `$13,370,000 → $12,033,000` shown on screen (exactly 90%). |
| Both button interactions | ✅ | Both reveals captured (`02_reveal_dontpay`, `03_reveal_pay`); neither sends a command. |
| Countdown animation | ✅ | Captured (`05_countdown`). |
| Mascot ("Elmo") animation | ✅ | Original procedural laughing mascot captured mid-laugh. |
| Flashing + reveal effects | ✅ | Visible in `03_reveal_pay` (red flash frame); gated by `flashingEffects`. |
| Sound effects | ✅ wired | Uses vanilla sound events via `SoundManager`; audio muted in headless capture. |
| Reconnecting to the server | ✅ logic | `ClientPlayConnectionEvents.DISCONNECT` resets state so the next join re-triggers. |
| Compatibility with other Fabric mods | ✅ | Client-only, **no mixins/access-wideners**; loaded cleanly with the entire Fabric API present. |

## Honesty note

What could **not** be exercised in this environment is an actual authenticated
connection to the live DonutSMP server (that requires a real Microsoft/Mojang login,
which isn't available to a headless build). The join-trigger and live balance parsing
are therefore validated by their code paths and the offline/demo path; the entire
interface, animations and rendering were validated in the **real** 1.21.11 client, and
the screenshots above are genuine in-game frames, not mock-ups.
