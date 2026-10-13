# StoreAnnouncer 🛒

A Paper +1.21.4 plugin that announces your Tebex store purchases in chat with the buyer's **real skin head drawn in pixel art**, and optionally triggers a **GG Wave** where players type `gg` to earn rewards.

## ✨ Features

- **Tebex integration** — works with a single console command added to your Tebex packages
- **Skin head in chat** — the buyer's real 8x8 skin face (with hat layer) drawn with `█` blocks, with the announcement text next to it
- **Hover text** — customizable tooltip when mousing over the head
- **Clickable store link** — final line that opens your store in the player's browser
- **MiniMessage support** — gradients, hex colors, click and hover events in every text
- **Offline buyers** — announcements still run when the buyer is offline (Tebex `#` prefix queues the command)
- **Smart price line** — hidden automatically when the price is `0` or the line is empty
- **Skin cache** — faces are cached for 10 minutes and downloaded asynchronously, with a fallback head if the download fails
- **Announcement sound** — configurable global sound
- **GG Wave** — timed event where players type `gg` in chat to earn rewards
  - Starts automatically on every purchase or manually with a command
  - `EXACT` or `CONTAINS` matching mode (`gg!`, `gg?`, `gg ez`...)
  - Console command rewards with `%player%` placeholder
  - Maximum rewards per player and anti-spam cooldown
  - First-time bonus commands
  - Rotating color chat format for participants — the color changes with every `gg`
  - Configurable start/end messages and sounds
- **Manual announcements** — announce any product from the game or console
- **Live reload** — `/store reload` applies config changes without restarting
- **Tab completion** for all commands
- **No external dependencies** — only requires Paper +1.21.4

## 📦 Dependencies

| Dependency | Type | Link |
|---|---|---|
| Paper +1.21.4 (Java 21) | Required | [papermc.io](https://papermc.io) |
| Tebex | Optional (for automatic announcements) | [tebex.io](https://www.tebex.io) |

No additional plugins required.

## 🔌 Tebex Setup

In the Tebex panel go to **Packages → your package → Commands** and add:

```
#store advertise {username} {packageName} {quantity} {price}
```

- With the `#` prefix the command runs even if the player is offline (queued until their next login).
- Without `#` it only runs if the player is online right now.

| Placeholder | Description |
|---|---|
| `{username}` | Buyer name |
| `{packageName}` | Purchased package name |
| `{quantity}` | Quantity purchased |
| `{price}` | Price paid |

## 🔧 Commands

| Command | Permission | Description |
|---|---|---|
| `/store reload` | `storeannouncer.admin` | Reloads config.yml |
| `/store advertise <player> <product...> [quantity] [price]` | `storeannouncer.admin` | Announces a purchase manually |
| `/store ggwave [seconds]` | `storeannouncer.admin` | Starts a manual GG Wave (uses the configured duration by default) |
| `/store ggwave stop` | `storeannouncer.admin` | Stops the active GG Wave |

The permission `storeannouncer.admin` is given to operators by default. The console can run every command.

## ⚙️ Full Configuration

```yaml
# ============================================================
#  StoreAnnouncer - Configuration
#  All texts use MiniMessage format:
#  https://docs.advntr.dev/minimessage/format.html
# ============================================================

# ------------------------------------------------------------
# TEBEX INTEGRATION
# ------------------------------------------------------------
# In the Tebex panel go to:  Packages -> your package -> Commands
# and add this command:
#
#    #store advertise {username} {packageName} {quantity} {price}
#
#  - The # prefix  -> runs even if the player is offline
#                     (queued until their next login)
#  - Without #     -> only runs if the player is online right now
#
# Tebex placeholders available:
#   {username}     -> buyer name
#   {packageName}  -> purchased package name
#   {quantity}     -> quantity purchased
#   {price}        -> price paid
#
# You can also announce manually from the game or console:
#   /store advertise <player> <product...> [quantity] [price]
#   /store reload          -> reloads this configuration
#   /store ggwave [sec]    -> starts a manual GG Wave
#   /store ggwave stop     -> stops the active GG Wave
# ------------------------------------------------------------

announcer:
  # Player head (real 8x8 skin pixels) drawn with █ blocks,
  # with the text next to it (right side, vertically centered)
  head:
    enabled: true

  # Global sound when a purchase is announced ("" to disable)
  # Examples: entity.player.levelup, block.note_block.pling, ui.toast.challenge_complete
  sound: "entity.player.levelup"

  # Your Tebex store link (clickable final line). Set to "" to hide the line
  store-link: "https://store.yourserver.com"

  # ── ANNOUNCEMENT TEXTS (shown next to the head) ──
  # Placeholder: %player% -> buyer name
  header: "<dark_gray>«</dark_gray> <bold><white>%player%</white></bold> <dark_gray>▶</dark_gray> <green><bold>has purchased in our store</bold></green>"

  # Placeholders: %product% -> package name | %quantity% -> quantity bought
  product: "<dark_gray>•</dark_gray> <aqua><bold>%product%</bold></aqua> <dark_gray>x%quantity%</dark_gray>"

  # Placeholder: %price% -> price paid
  # (this line is hidden automatically if the price is 0, or if you set "")
  price: "<dark_gray>•</dark_gray> <gold>$%price%</gold>"

  # Placeholder: %link% -> replaced by the store-link value above
  # The line is clickable and opens the store in the player's browser
  store-line: "<click:open_url:'%link%'><gradient:#00FFA3:#F5D90A><bold>➥ Visit our store!</bold></gradient></click>"

# ------------------------------------------------------------
# GG WAVE
# ------------------------------------------------------------
ggwave:
  # Start a GG Wave automatically every time a purchase is announced
  auto-start: true

  # Wave duration in seconds
  duration-seconds: 30

  # Sound when the wave starts ("" to disable)
  start-sound: "entity.ender_dragon.growl"

  # Wave start message
  # Placeholder: %time% -> how many seconds the wave lasts
  start-message: "<gradient:#FF5555:#FFAA00><bold>GG WAVE!</bold></gradient> <yellow>Type</yellow> <green><bold>gg</bold></green> <yellow>in chat to earn rewards</yellow> <gray>(%time%s)</gray>"

  # Wave end message ("" to disable)
  end-message: "<dark_gray>The</dark_gray> <gradient:#FF5555:#FFAA00><bold>GG Wave</bold></gradient> <dark_gray>has ended, thanks for participating!</dark_gray>"

  reward:
    # EXACT    -> only counts the "gg" message (with or without symbols: gg!, gg?)
    # CONTAINS -> counts any message containing "gg" (e.g. "gg ez")
    mode: EXACT

    # Console commands executed on reward
    # Placeholder: %player% -> rewarded player name
    # Examples with other plugins:
    #   - "eco give %player% 100"           (EssentialsX)
    #   - "crate give %player% normal 1"    (CrazyCrates)
    commands:
      - "give %player% diamond 1"

    # Maximum rewards a player can receive within a single wave
    max-per-player: 3

    # Minimum seconds between rewards (anti-spam)
    cooldown-seconds: 5

    first-time-bonus:
      # Extra bonus ONLY the first time the player types gg in the wave
      enabled: true
      # Placeholder: %player%
      commands:
        - "give %player% gold_nugget 8"

    # Sound on receiving a reward ("" to disable)
    sound: "entity.experience_orb.pickup"

    # Private message on reward
    # Placeholders: %done% -> rewards received | %max% -> maximum allowed
    message: "<green>You received a reward for your</green> <bold>gg</bold><green>!</green> <dark_gray>(%done%/%max%)</dark_gray>"

  # Chat format for participants while the wave is running
  # (activates when the player types their first "gg")
  # Placeholders:
  #   {color}    -> the player's current color (rotates with each "gg" they type)
  #   %name%     -> player name
  #   %message%  -> message content (REQUIRED!)
  chat-format: "{color}<bold>%name%</bold> <dark_gray>»</dark_gray> <white>%message%</white>"

  # Hex color palette that rotates with each "gg" typed
  # (1st gg = color 1, 2nd gg = color 2... loops back when the list ends)
  colors:
    - "#FF5555"
    - "#FFAA00"
    - "#FFFF55"
    - "#55FF55"
    - "#55FFFF"
    - "#5555FF"
    - "#FF55FF"

# ------------------------------------------------------------
# PLUGIN MESSAGES (admin feedback + head hover)
# ------------------------------------------------------------
messages:
  usage: "<red>Usage:</red> <gray>/store <reload | advertise | ggwave></gray>"
  usage-advertise: "<red>Usage:</red> <gray>/store advertise <player> <product...> [quantity] [price]</gray>"
  reloaded: "<green>StoreAnnouncer configuration reloaded.</green>"
  announced: "<green>Announcement sent.</green>"
  wave-stopped: "<green>GG Wave stopped.</green>"
  # Hover text shown when mousing over the skin head. %player%
  head-hover: "<gradient:#FFD700:#FF8C00><bold>Purchase made by %player%!</bold></gradient>"
```

## 🎨 Text Format

All texts use [MiniMessage](https://docs.advntr.dev/minimessage/format.html):

```yaml
header: "<bold><white>%player%</white></bold> <green>has purchased in our store</green>"   # Named colors
price: "<gold>$%price%</gold>"                                                              # Placeholders
store-line: "<click:open_url:'%link%'><gradient:#00FFA3:#F5D90A>Visit our store!</gradient></click>"  # Gradient + click
colors:
  - "#FF5555"   # Hex colors for the GG chat rotation
```

## 🏆 GG Wave

```yaml
ggwave:
  auto-start: true
  duration-seconds: 30
  reward:
    mode: EXACT
    commands:
      - "give %player% diamond 1"
    max-per-player: 3
    cooldown-seconds: 5
```

With `mode: EXACT` only the `gg` message counts (symbols like `gg!` or `gg?` are ignored). With `mode: CONTAINS` any message containing `gg` counts. Each player can receive up to `max-per-player` rewards per wave, with at least `cooldown-seconds` between them. The first reward of a wave also runs the `first-time-bonus` commands.

## 📁 Generated Files

```
plugins/StoreAnnouncer/
└── config.yml       # Plugin configuration
```

## ⚠️ Notes

- Skin heads are downloaded from external services (`mc-heads.net` and the Mojang API), so your server needs outbound internet access. If the download fails, a fallback head is shown.
- Rewards are executed as console commands, so they work with any economy or crate plugin.

