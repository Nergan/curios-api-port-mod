# Curios API — инструкции для AI-агентов

Порт Curios API на Fabric 1.21.1. Только Fabric. Второй загрузчик не добавлять. `mod_id` — `curios`. Пакет своего кода — `com.curios.mod`. Публичный API остаётся в `top.theillusivec4.curios.api`, чтобы чужой код находил те же имена.

Лицензия исходника — LGPL-3.0-or-later. На MPL-2.0 не перелицензировать. На Modrinth и на Mikchan CDN не публиковать. Jar лежит в GitHub Release `minecraft-1.21.1-fabric`. `.github/workflows/sync-to-gitlab.yml` зеркалит репозиторий и этот релиз в `naragan/curios-api-port-mod`: на GitLab те же название и текст, а файлы — ссылки на скачивание GitHub. Второй jar не заливается. Это не пакет `modpacks-curios`: тот остаётся пропатченным curiosfabric и в сборку не входит. Сборка Kidou берёт GitHub Release через `type: github`. У Kidou 1.1.4 нет типа `gitlab`.

`org.gradle.jvmargs` остаётся `-Xmx2G`. Wrapper не добавлять. Сборка — Gradle 8.11 и JDK 21.

## Локальные инструменты

Kidou — локальный инструмент этой мастерской. Искать сведения о нём в интернете бесполезно: публичной документации нет. Источник — спецификация в `D:\DATA\soft\code\projects\minecraft\modpacks\Kidou-1.1.8-release.jar` и `D:\DATA\soft\code\projects\minecraft\modpacks\AGENTS.md`.

Модель игрока рисует надетые предметы из вложения `curios:inventory`. Список слотов датапака на выделенном клиенте пуст: его читает только серверный reload. Рендер идёт по предметам во вложении, не по `CuriosRuntime.slotsFor`. Окно слотов берёт раскладку из пакета меню, поэтому оно работает и без этого списка.

## Проверка мода и сборки

Работа над модом или сборкой заканчивается только после тестов, проверки запуском локального сервера и локального клиента. Сборка jar без этого не считается готовой.

Из этого каталога: `gradle test`, затем `gradle runClient` до главного меню, затем `gradle runServer` до строки `Done`. Окно и сервер после этого закрыть.
