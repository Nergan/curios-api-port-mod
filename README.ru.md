# Curios API

[English](README.md)

Порт API слотов Curios на Fabric 1.21.1. Мод читает те же датапаки `curios/slots` и `curios/entities` и те же теги предметов `curios`, что Curios 9.5.1. Это мод Fabric, не коннектор NeoForge и не миксин поверх чужого jar Curios.

Идентификатор мода — `curios`. Предмет со тегом `curios:<слот>` или `curios:curio` встаёт в слот, если валидатор слота — `curios:tag`. Слоты открываются клавишей G.

Artifacts 13.2.4 для Fabric по-прежнему включает способности через Accessories или Trinkets. Этот порт даёт таким предметам слот и не запускает эффекты артефактов.

## Загрузчики

Только Fabric 1.21.1. Сборки NeoForge в этом репозитории нет.

## Загрузки

GitHub Release `minecraft-1.21.1-fabric`: `curios-fabric-1.21.1-1.0.2.jar`.

`*-sources.jar` в релиз не входит и в `mods` не кладётся.

На Modrinth и на Mikchan CDN этот порт не публикуется.

## Требования

- Minecraft 1.21.1
- Fabric Loader 0.16.14 или новее
- Fabric API 0.116.17+1.21.1
- Fabric Language Kotlin 1.13.2+kotlin.2.1.20
- Java 21

## Установка

Положите jar в `mods` Fabric. Состав слотов берётся из датапаков. Файлы в самом моде задают обычные слоты Curios. Другой мод, например Artifacts, добавляет список слотов игрока и может изменить размер слота.

## Конфиг

Файла настроек нет. Клавиша `curios.key.open` по умолчанию G.

## Лицензия

LGPL-3.0-or-later, та же лицензия, что у Curios автора TheIllusiveC4. Этот репозиторий её не меняет.
