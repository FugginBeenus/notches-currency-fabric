# Notch Currency Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.6-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/FugginBeenus/notch-currency-fabric)](https://github.com/FugginBeenus/notch-currency-fabric/releases)

A server-side economy mod for Fabric 1.20.1 with ATMs, player trading, and NPC shop integration.

> Heavily inspired by the Origin Realms economy system. The core concept isn't mine, but all the code and assets are written from scratch. I'm still fairly new to modding so development may be slow — open to collaboration if anyone's interested.

## Features

### Banking
- Physical ATM blocks for depositing and withdrawing currency
- Auto-deposit when Notch Chips are placed in an ATM
- Server-side transaction validation
- Persistent balance storage with backups

### Trading
- Secure player-to-player item and currency exchange
- Both parties must confirm before a trade executes
- Configurable max trading distance
- Compatible with Shopkeeper mod — NPC trades update player balances

### HUD
- Always-visible balance display with configurable position
- Smooth balance transition animations

## Development Setup

1. Import the project into IntelliJ IDEA
2. Run `./gradlew genSources`
3. Refresh the Gradle project
4. Run the `Minecraft Client` configuration

## Links

- [Modrinth](https://modrinth.com/project/notches-currency)
- [CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/notches-currency)
- [Discord](https://discord.gg/fMpb6retYA)
- [Wiki](https://github.com/FugginBeenus/notches-currency-fabric/wiki)

## Credits

- **FugginBeenus** — initial work and maintenance
- **Player Trade Mod** — UI inspiration
- **Origin Realms** — economy system inspiration

## Contributing

Bug reports and feature requests are welcome via GitHub Issues. PRs are open if you want to contribute code.
