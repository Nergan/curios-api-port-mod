# Curios API — инструкции для AI-агентов

Порт Curios API на Fabric 1.21.1. Только Fabric. Второй загрузчик не добавлять. `mod_id` — `curios`. Пакет своего кода — `com.curios.mod`. Публичный API остаётся в `top.theillusivec4.curios.api`, чтобы чужой код находил те же имена.

Лицензия исходника — LGPL-3.0-or-later. На MPL-2.0 не перелицензировать. На Modrinth не публиковать. GitHub Release `minecraft-1.21.1-fabric` и публичный пакет McnCDN `curios-api-port-mod` (слот `jar`, версия равна `mod_version`) держат один и тот же jar. Это не пакет `modpacks-curios`: тот остаётся пропатченным curiosfabric и в сборку не входит. Опубликованный номер CDN не перезаписывается, поэтому следующий jar требует больший `mod_version`.

`org.gradle.jvmargs` остаётся `-Xmx2G`. Wrapper не добавлять. Сборка — Gradle 8.11 и JDK 21.

## Локальные инструменты

Kidou и Mikchan CDN — локальные инструменты этой мастерской. Искать сведения о них в интернете бесполезно: публичной документации нет. Для Kidou источник — спецификация в `D:\DATA\soft\code\projects\minecraft\modpacks\Kidou-1.1.4-release.jar` и `D:\DATA\soft\code\projects\minecraft\modpacks\AGENTS.md`. Для Mikchan CDN — https://cdn.mikchan.net/ai/SKILL.md и `GET https://cdn.mikchan.net/version`.

## Проверка мода и сборки

Работа над модом или сборкой заканчивается только после тестов, проверки запуском локального сервера и локального клиента. Сборка jar без этого не считается готовой.

Из этого каталога: `gradle test`, затем `gradle runClient` до главного меню, затем `gradle runServer` до строки `Done`. Окно и сервер после этого закрыть.
