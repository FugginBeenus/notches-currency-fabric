# 💰 Notch Currency Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.6-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/FugginBeenus/notch-currency-fabric)](https://github.com/FugginBeenus/notch-currency-fabric/releases)

A comprehensive server-side economy mod for Minecraft Fabric 1.20.1 that introduces a secure currency system with ATMs, player trading, and NPC integration support.

## ✨ Features

### 🏦 Banking System
- **ATM Blocks** - Physical ATM machines for depositing and withdrawing currency
- **Auto-deposit** - Automatically converts Notch Chips when placed in ATMs
- **Secure Transactions** - All currency operations are server-side validated
- **Persistent Storage** - Player balances are saved and backed up

### 💱 Trading System  
- **Player-to-Player Trading** - Secure item and currency exchange between players
- **Trade GUI** - Clean, intuitive interface inspired by popular trading mods
- **Trade Safety** - Both parties must confirm before trade execution
- **Distance Limits** - Configurable maximum trading distance

### 📊 HUD & Display
- **Balance HUD** - Always-visible currency display (configurable position)
- **Animated Balance** - Smooth transitions when balance changes
- **Custom Formatting** - Proper number formatting with configurable currency names

### 🎮 Commands

#### Player Commands
- `/bal` - Check your current balance
- `/pay <player> <amount>` - Send Notches to another player
- `/trade <player>` - Initiate a trade with another player

#### Admin Commands
- `/eco give <player> <amount>` - Give Notches to a player
- `/eco take <player> <amount>` - Remove Notches from a player
- `/eco set <player> <amount>` - Set a player's balance
- `/eco reload` - Reload the configuration

### 🔧 NPC Integration
Simple API for custom NPCs to interact with the economy:
```java
// Check balance
long balance = NotchCurrency.getEconomy().getBalance(player);

// Charge player
boolean success = NotchCurrency.getEconomy().withdraw(player, amount, "NPC Purchase");

// Pay player
NotchCurrency.getEconomy().deposit(player, amount, "NPC Sale");
```

### 📝 Audit Logging
- Comprehensive transaction logging to CSV
- Track all deposits, withdrawals, transfers, and trades
- Admin actions logged separately
- Configurable via config file

## 📦 Installation

### For Players
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.20.1
2. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) 0.92.6 or newer
3. Download the latest mod JAR from [Releases](https://github.com/FugginBeenus/notch-currency-fabric/releases)
4. Place the JAR in your `mods` folder
5. Launch Minecraft!

### For Server Administrators
1. Install Fabric server for 1.20.1
2. Add Fabric API and Notch Currency mod to the server's `mods` folder
3. Configure settings in `config/notch-currency.json` after first launch
4. Set appropriate permissions for admin commands

## ⚙️ Configuration

The mod creates a `config/notch-currency.json` file with these options:

```json
{
  "startingBalance": 100,
  "transferFeePercent": 0.0,
  "tradeTaxPercent": 0.0,
  "enableAuditLog": true,
  "enableHUD": true,
  "hudPosition": "TOP_LEFT",
  "allowATMCrafting": true,
  "maxTradeDistance": 10,
  "maxTransferAmount": 1000000,
  "enableTradeCommand": true,
  "enablePayCommand": true,
  "currencyNameSingular": "Notch",
  "currencyNamePlural": "Notches"
}
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `startingBalance` | Notches given to new players | 100 |
| `transferFeePercent` | Fee charged on `/pay` transfers | 0.0 |
| `tradeTaxPercent` | Tax on player trades | 0.0 |
| `enableAuditLog` | Log all transactions to CSV | true |
| `hudPosition` | HUD position (TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT) | TOP_LEFT |
| `maxTradeDistance` | Maximum distance for trading (blocks) | 10 |

## 🎨 Crafting Recipes

### ATM Block
```
[I] [R] [I]
[I] [C] [I]
[I] [I] [I]

I = Iron Ingot
R = Redstone
C = Chest
```

### Notch Chip
```
[G] [G] [G]
[G] [E] [G]
[G] [G] [G]

G = Gold Nugget
E = Emerald
```

## 🔌 API Usage

### For Mod Developers

Add to your `build.gradle`:
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    modImplementation "com.github.FugginBeenus:notch-currency-fabric:${notch_version}"
}
```

### Example Integration
```java
import net.fugginbeenus.notchcurrency.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionResult;

public class MyNPCShop {
    public void sellItem(ServerPlayerEntity player, ItemStack item, long price) {
        // Check if player can afford it
        if (!NotchCurrency.getEconomy().canAfford(player, price)) {
            player.sendMessage(Text.literal("Insufficient Notches!"));
            return;
        }
        
        // Charge the player
        TransactionResult result = NotchCurrency.getEconomy()
            .withdraw(player, price, "Bought " + item.getName());
            
        if (result.success()) {
            // Give item to player
            player.giveItemStack(item);
            player.sendMessage(Text.literal("Purchase successful!"));
        }
    }
}
```

## 📸 Screenshots

![ATM Interface](https://via.placeholder.com/400x300?text=ATM+Interface)
*Clean ATM interface for managing your Notches*

![Trading GUI](https://via.placeholder.com/400x300?text=Trading+GUI)
*Secure player-to-player trading system*

![HUD Display](https://via.placeholder.com/400x100?text=Balance+HUD)
*Customizable HUD showing your current balance*

## 🏗️ Building from Source

```bash
# Clone the repository
git clone https://github.com/FugginBeenus/notch-currency-fabric.git
cd notch-currency-fabric

# Build the mod
./gradlew build

# Output JAR will be in build/libs/
```

### Development Setup
1. Import the project into IntelliJ IDEA
2. Run `./gradlew genEclipseRuns` or `./gradlew genSources`
3. Refresh the Gradle project
4. Run the `Minecraft Client` configuration

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/FugginBeenus/notch-currency-fabric/issues) page to report bugs or request features.

When reporting bugs, please include:
- Minecraft version
- Fabric version
- Mod version
- Crash log (if applicable)
- Steps to reproduce

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👏 Credits

- **FugginBeenus** - *Initial work and maintenance*
- **Fabric Team** - *For the amazing modding platform*
- **Player Trade Mod** - *UI inspiration*
- **Origin Realms** - *Economy system inspiration*

## 📚 Links

- [CurseForge Page](https://www.curseforge.com/minecraft/mc-mods/notch-currency)
- [Modrinth Page](https://modrinth.com/mod/notch-currency)
- [Discord Server](https://discord.gg/your-discord)
- [Wiki](https://github.com/FugginBeenus/notch-currency-fabric/wiki)

## 🌟 Support

If you enjoy this mod, consider:
- ⭐ Starring the repository
- 🐛 Reporting bugs and issues
- 💡 Suggesting new features
- 🤝 Contributing code
- ☕ [Buying me a coffee](https://ko-fi.com/fugginbeenus)

---

Made with ❤️ for the Minecraft community
