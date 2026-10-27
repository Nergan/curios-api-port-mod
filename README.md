# Curios API

[Русский](README.ru.md)

Fabric 1.21.1 port of the Curios slot API. It reads the same `curios/slots` and `curios/entities` datapacks and the same `curios` item tags as Curios 9.5.1. It is a Fabric mod, not a NeoForge connector and not a mix-in over another Curios jar.

The mod id is `curios`. Items whose tag is `curios:<slot>` or `curios:curio` can be placed when the slot validator is `curios:tag`. Press G to open the slots.

Artifacts 13.2.4 for Fabric still runs its abilities through Accessories or Trinkets. This port gives those items a slot. It does not run the artifact effects.

## Loaders

Fabric 1.21.1 only. There is no NeoForge build in this repository.

## Downloads

GitHub Release `minecraft-1.21.1-fabric`: `curios-fabric-1.21.1-1.0.3.jar`. The same release is mirrored to GitLab [naragan/curios-api-port-mod](https://gitlab.com/naragan/curios-api-port-mod/-/releases/minecraft-1.21.1-fabric). The files there link to the GitHub downloads.

`*-sources.jar` is not part of the release and does not go in `mods`.

This port is not published to Modrinth or Mikchan CDN.

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.14 or newer
- Fabric API 0.116.17+1.21.1
- Fabric Language Kotlin 1.13.2+kotlin.2.1.20
- Java 21

## Install

Put the jar in the Fabric `mods` folder. Slot layout comes from datapacks. The bundled files add the usual Curios slots. Another mod, such as Artifacts, adds the player entity list and can change a slot size.

## Config

There is no config file. The key `curios.key.open` defaults to G.

## License

LGPL-3.0-or-later, the same license as Curios by TheIllusiveC4. This repository does not relicense that code.
