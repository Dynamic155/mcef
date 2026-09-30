<p align="center">
  <img src="https://github.com/CinemaMod/mcef/assets/30220598/938896d7-2589-49df-8f82-29266c64dfb7" alt="MCEF Logo" style="width:66px;height:66px;">
</p>

# MCEF (Minecraft Chromium Embedded Framework)
MCEF is a mod and library for adding the Chromium web browser into Minecraft.

MCEF is based on java-cef (Java Chromium Embedded Framework), which is based on CEF (Chromium Embedded Framework), which is based on Chromium. It was originally created by montoyo. It was rewritten and currently maintained by the CinemaMod Group.

MCEF contains a downloader system for downloading the java-cef & CEF binaries required by the Chromium browser. This requires a connection to https://mcef-download.cinemamod.com.

Discussion: https://discord.gg/rNrh5kW8Ty

Current Chromium version: `116.0.5845.190`

## This fork
This is a fork of [CinemaMod/mcef](https://github.com/CinemaMod/mcef), branched from `1.21.5`, ported to run on Minecraft 26.2 / NeoForge. It is not published anywhere; if you need it, clone this repo and build it yourself (see "Building & Modifying MCEF" below).

Fabric support is dropped for now. Fabric Loom's bundled ASM can't parse Minecraft 26.2's class files yet, so the `fabric` module doesn't build on this branch and has been removed from `settings.gradle`. NeoForge is unaffected.

### What changed for 26.2
Minecraft 26.2 replaced a lot of the rendering internals MCEF depends on, so most of the porting work was in the texture upload path and the GUI code:

- Texture upload moved off raw OpenGL calls (`glTexImage2D`/`glTexSubImage2D`) and onto the new `GpuTexture`/`GpuFormat`/`CommandEncoder#writeToTexture` API. This also means the upload path now works under both the OpenGL and the new experimental Vulkan renderer, not just OpenGL.
- CEF hands over BGRA8 pixel data, and the new upload API doesn't do format conversion for you, so there's now a CPU-side repack step that swaps the byte order before each upload.
- GUI code was moved off the old immediate-mode `Screen#render` onto the new retained-mode `Screen#extractRenderState`, and off `PoseStack` onto `Matrix3x2fStack`.
- `ResourceLocation` was renamed to `Identifier`, `Minecraft#setScreen` moved to `Gui#setScreen`, and mouse/keyboard input moved to the new `KeyEvent`/`MouseButtonEvent`/`CharacterEvent` types.
- `CefInitMixin` now targets `Gui` instead of `Minecraft`, since that's where screen-opening logic lives now.
- Fixed a cursor bug where `glfwCreateStandardCursor(0)` was being called for cursor types with no GLFW standard-cursor equivalent (the default arrow cursor among them), which raised a GL error every time the cursor changed. This was a pre-existing bug, not something introduced by the port.
- The demo mod's F10 keybind was created but never actually registered with `RegisterKeyMappingsEvent`, so it never worked even upstream. It's registered now, and there's also an `/mcefdemo` client command that opens the demo browser directly.
- Build tooling bumped to Gradle 9.2.1, NeoGradle 7.1.39, and JDK 25 (Minecraft 26.2 ships Java 25 to end users). There are no Parchment mappings published for 26.2 yet, so this branch compiles against plain official mappings for now.

Verified in a NeoForge 26.2 dev client: CEF initializes, downloads and loads its native binaries, and the demo browser renders and handles real page interaction (navigation, search, cursor changes) across multi-hour sessions with no crashes.

## Supported Platforms
- Windows 10/11 (x86_64, arm64)*
- macOS 11 or greater (Intel, Apple Silicon)
- GNU Linux glibc 2.31 or greater (x86_64, arm64)**

*Some antivirus software may prevent MCEF from initializing. You may have to disable your antivirus or whitelist the mod files for MCEF to work properly.

**This mod will not work on Android.

## For Players
This is the source code for MCEF.

For the upstream mod (Fabric or NeoForge, up to Minecraft 1.21.4), download it from either:
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/mcef
- Modrinth: https://modrinth.com/mod/mcef

This fork isn't published there. If you need MCEF on 26.2, build this branch yourself.

## For Modders
MCEF is LGPL, as long as your project doesn't modify or include MCEF source code, you can choose a different license. Read the full license in the LICENSE file in this directory.

### Using upstream MCEF in your project
This applies to the upstream project, up to Minecraft 1.21.4. This fork has no published maven artifacts; depend on it as a local jar instead (build it with `./gradlew :neoforge:jar`, then point your project at the jar under `neoforge/build/libs/`).
```
repositories {
    maven {
        url = uri('https://mcef-download.cinemamod.com/repositories/releases')
    }
    // Optional for snapshot versions
    maven {
        url = uri('https://mcef-download.cinemamod.com/repositories/snapshots')
    }
}
```
#### Fabric
```
dependencies {
    modCompileOnly 'com.cinemamod:mcef:2.1.6-1.21.4'
    modRuntimeOnly 'com.cinemamod:mcef-fabric:2.1.6-1.21.4'
}
```
See the [mcef-fabric-example-mod](https://github.com/CinemaMod/mcef-fabric-example-mod) for a complete example Fabric project.

#### NeoForge
```
dependencies {
    compileOnly fg.deobf('com.cinemamod:mcef:2.1.6-1.21.4')
    runtimeOnly fg.deobf('com.cinemamod:mcef-neoforge:2.1.6-1.21.4')
}
```
### Building & Modifying MCEF
After cloning this repo, you will need to clone the java-cef git submodule. There is a gradle task for this: `./gradlew cloneJcef`.

To run the NeoForge client: `./gradlew neoforgeClient`

(On this fork, `fabricClient` won't work; see "This fork" above.)

In-game, there is a demo browser if you press F10 after you're loaded into a world, or by running `/mcefdemo` (both only exist when running from a development environment).
