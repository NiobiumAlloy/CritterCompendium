# Critter Compendium

A client-side Fabric mod designed to help with unique safari runs by tracking the critters that have been caught, announcing when zones are done, and offering both a client side command and a party command to display what critters have yet to be caught 

This mod DOES NOT locate/highlight any critters or floordrops, only keeps track of caught critter messages in chat and compare that to a list of all critters 

Currently only supports version 26.1.2

---

## Commands

### Client Commands

| Command                   | Description                                                                                                                                      | Default |
|:--------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `/chelp`                  | Displays the help menu listing available commands.                                                                                               |         |
| `/ctoggle`                | Enables/Disables the mod other than /chelp and /ctoggle                                                                                          | True    |
| `/c m [zone]`             | outputs the critters missing in a specific zone (`c.*`, `f.*`, `h.*`, `i.*`) or all zones if omitted to client side chat                         |         |
| `/c anounceZoneCompleted` | Exactly what is sounds like                                                                                                                      | True    |
| `/c includeTimestamps`    | Includes time to complete with zone/all completed messages                                                                                       | False   |
| `/c announceMacaw`        | Sends /pc message when a macaw spawns (known bug that if a macaw is alive as you get warped into another run it will announce the message again) | False   |
| `/c announceHotspot`      | Toggles automatic party chat announcement when your hotspot is revealed.                                                                         | False   |
| `/c enablePartyCommand`   | Toggles the party command                                                                                                                        | True    |
| `/c outputDebugToChat`    | Toggles debug chat messages on critter catch.                                                                                                    | False   |


---

## Party Command

When party chat tracking is active, players can trigger automated responses by typing in party chat:

- **`!m`** or **`!missing`**  
  Broadcasts the list of missing critters across all incomplete zones to party chat.
- **`!m <zone>`** or **`!missing <zone>`**  
  Broadcasts missing critters for a specific zone:
  - `!m cavern` / `!m c`
  - `!m forest` / `!m f`
  - `!m haunted` / `!m h`
  - `!m icy` / `!m i`

---

## Configuration Files

The mod creates and manages configurations inside your `.minecraft/config/` directory:

1. **`config/crittercompendium.json`**  
   Mod config file
2. **`config/crittercompendium-safari-messages.json`**  
   Stores customizable completion messages sent when a zone is finished.

---

## Zones & Unique Critters

<details>
<summary>Click to view the zone critter list</summary>

- **Cavern:** Cavernfish, Chuckwalla, Driftling, Gemzie, Rockmite, Scrappy, Shyworm, Snoozle, Flitter
- **Forest:** Bluebird, Fluffling, Foxtrot, Hideonfloor, Honeybug, Parakeet, Treefrog, Woodchucker *(Macaw is tracked optionally)*
- **Haunted:** Areita, Bloodbat, Doomspiral, Duplico, Gazer, Gimmiegold, Hideonwall, Hideyho, Litterbug, Solsnatcher
- **Icy:** Billygoat, Mantis Shrimp, Nozzlenose, Polaris, Shuddersquid, Strongarm, Tepid, Troodon, Wumpa

</details>

---

## Requirements

- **Minecraft version 26.1.2**
- **Fabric Loader**
- **Fabric API**

## License

All rights reserved.
See [License](LICENSE.txt)
