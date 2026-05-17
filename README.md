# GravesXModule-Economy

A [GravesX](https://github.com/Legoman99573/GravesX) module that charges players Vault economy currency for interacting with their graves — fully compatible with **Folia**, **Paper**, and **Spigot**.

---

## Features

- Charge players for **teleporting**, **opening**, **auto-looting**, and **breaking** graves
- Two charge modes per action: **fixed amount** or **percentage of balance**
- Per-permission charge overrides (`graves.economy.chargebypass.<type>.<amount>`)
- Bypass permissions per action so staff/donors can skip charges
- **PlaceholderAPI** support — expose costs in scoreboards, menus, and chat
- Full **i18n** — drop in any language file, players see messages in their client locale
- **Folia-safe** — no BukkitScheduler usage, all scheduling via GravesX's own Folia-aware scheduler

---

## Requirements

| Dependency | Version | Required |
|---|---|---|
| Paper / Folia / Spigot | 1.21.4+ | ✓ |
| GravesX | 4.9.10.10+ | ✓ |
| Vault | any | ✓ |
| Economy plugin (EssentialsX, CMI, etc.) | any | ✓ |
| PlaceholderAPI | 2.11.6+ | ✗ optional |

---

## Installation

1. Place `EconomyVault-<version>.jar` inside `plugins/GravesX/modules/`
2. Restart the server
3. Edit `plugins/GravesX/modules/Economy-Vault/config.yml`
4. Run `/graveecon reload` to apply changes without a restart

---

## Configuration

`plugins/GravesX/modules/Economy-Vault/config.yml`

```yaml
enabled: true
default-language: "en_us"

economy:
  currency-symbol: "$"
  round-to-decimals: 2

types:
  TELEPORT:
    enabled: true
    charge:
      mode: FIXED          # FIXED or PERCENT_BALANCE
      fixed: 50.0          # used when mode is FIXED
      percent: 1.0         # used when mode is PERCENT_BALANCE

  OPEN:
    enabled: true
    charge:
      mode: FIXED
      fixed: 25.0
      percent: 0.5

  AUTOLOOT:
    enabled: true
    charge:
      mode: FIXED
      fixed: 35.0
      percent: 0.75

  BLOCK_BREAK:
    enabled: true
    charge:
      mode: FIXED
      fixed: 100.0
      percent: 2.0
```

### Charge modes

| Mode | Behaviour |
|---|---|
| `FIXED` | Deducts a flat amount. For `TELEPORT`, the amount is multiplied by the distance in blocks. |
| `PERCENT_BALANCE` | Deducts a percentage of the player's current balance. |

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `graves.economy.reload` | Reload the module config via `/graveecon reload` | OP |
| `graves.economy.teleport` | Bypass teleport charge | `false` |
| `graves.economy.open` | Bypass open charge | `false` |
| `graves.economy.autoloot` | Bypass auto-loot charge | `false` |
| `graves.economy.block_break` | Bypass block-break charge | `false` |

### Per-player charge overrides

Give a player a permission in the format `graves.economy.chargebypass.<type>.<amount>` to override their charge for a specific action. The lowest value among all matching permissions wins.

**Examples:**
```
graves.economy.chargebypass.teleport.10     → charged exactly $10 to teleport
graves.economy.chargebypass.open.0          → free grave opening
graves.economy.chargebypass.block_break.50  → charged $50 to break
```

---

## Commands

| Command | Description |
|---|---|
| `/graveecon reload` | Reload config without restarting |

Aliases: `/gravesxecon`, `/gecon`

---

## PlaceholderAPI

| Placeholder | Returns |
|---|---|
| `%graves_currency_symbol%` | Configured currency symbol |
| `%graves_teleport_cost%` | Fixed cost for teleporting |
| `%graves_teleport_cost_percentage%` | Percentage cost for teleporting |
| `%graves_open_cost%` | Fixed cost for opening |
| `%graves_open_cost_percentage%` | Percentage cost for opening |
| `%graves_autoloot_cost%` | Fixed cost for auto-looting |
| `%graves_block_break_cost%` | Fixed cost for breaking |

---

## Language Files

Language files live in `plugins/GravesX/modules/Economy-Vault/languages/`.
`en_us.yml` and `es_es.yml` are extracted automatically on first load.

To add a new language, create `<locale>.yml` in that folder using the same key structure:

```yaml
graves:
  economy:
    teleport:
      charged: "Charged {currency}{amount} for teleporting."
      insufficient: "You need {currency}{amount} to teleport."
      failed: "Payment failed for teleporting. Please try again."
    open:
      charged: "Charged {currency}{amount} for opening a grave."
      insufficient: "You need {currency}{amount} to open a grave."
      failed: "Payment failed for opening a grave. Please try again."
    autoloot:
      charged: "Charged {currency}{amount} for auto-looting."
      insufficient: "You need {currency}{amount} to auto-loot."
      failed: "Payment failed for auto-looting. Please try again."
    block_break:
      charged: "Charged {currency}{amount} for breaking a grave."
      insufficient: "You need {currency}{amount} to break a grave."
      failed: "Payment failed for breaking a grave. Please try again."
```

Available tokens: `{currency}`, `{amount}`, `{type}`

Players automatically receive messages in their client locale. Falls back to `default-language` if their locale file is missing.

---

## Folia Support

This fork adds full Folia compatibility over the original module:

- `supportsFolia: true` declared in `module.yml`
- All scheduling routed through GravesX's Folia-aware `SchedulerManager` via `ctx.runTask()`
- Language files correctly extracted to and read from the module's own data folder
- Compiled against GravesX `4.9.10.10` which introduced `GravePostTeleportEvent` and other events used by this module

---

## Building

```bash
# Install the GravesX jar to your local Maven repo first
mvn install:install-file \
  -Dfile=GravesX-4.9.10.10.jar \
  -DgroupId=com.ranull \
  -DartifactId=GravesX \
  -Dversion=4.9.10.10 \
  -Dpackaging=jar

mvn clean package
```

Output: `target/EconomyVault-<version>.jar`

---

## Credits

Original module by [Legoman99573](https://github.com/Legoman99573), [Ranull](https://github.com/Ranull), and [JaySmethers](https://github.com/JaySmethers).
Folia compatibility possible by [https://github.com/jayesh-kambli](https://github.com/jayesh-kambli).
