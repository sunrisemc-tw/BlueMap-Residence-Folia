# BlueMap-Residence (Folia fork)

A Folia-safe fork of **BlueMap_Residence** v3.1.1 (SpigotMC resource
[107389](https://www.spigotmc.org/resources/107389/), by CZMixer), maintained by **sunrisemc-tw** for
the Sunrise MC server running [Canvas](https://github.com/CraftCanvasMC/Canvas) (a Paper/Folia fork).

## Why this fork exists

The upstream 3.1.1 jar declared `folia-supported: true` but was **not** actually Folia-safe: its periodic
marker refresh ran on the **global region thread** and walked every residence computing polygon marker
geometry there. On a server with many residences this stalled the global region for ~55 seconds and
tripped the Folia watchdog, forcing a restart.

## What changed

The heavy work was moved off the global region thread:

- **Phase 1 – global region thread (light):** read Residence data + Bukkit `Location`s + config and
  resolve PlaceholderAPI into a flat list of plain `MarkerData` snapshots.
- **Phase 2 – async scheduler (heavy):** tessellate shapes, build the BlueMap markers and write them into
  BlueMap's (thread-safe) marker sets.

A coalescing single-flight guard keeps at most one refresh in flight so the periodic timer and the
Residence change events can never run two refreshes concurrently. The periodic trigger itself now runs on
the async scheduler instead of the global region fixed-rate timer.

All original behaviour and `config.yml` compatibility are preserved.

## Building

```bash
./gradlew build
```

The plugin jar is written to `build/libs/BlueMap-Residence-Folia-3.1.1.jar`.

Requires JDK 21. Dependencies (`BlueMap`, `Residence`, `PlaceholderAPI`, Paper API) are all `compileOnly`
and provided by the server at runtime. The closed-source Residence API is supplied at compile time by the
minimal stubs under `src/stub/java` (these are **not** packaged into the jar).

## Credits

Original plugin © CZMixer. This fork only adapts the plugin for correct Folia scheduling.
