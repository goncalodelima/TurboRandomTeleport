# TurboRandomTeleport

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

TurboRandomTeleport is a high-performance random teleport plugin for Minecraft worlds, designed for fast and reliable teleportation across your server.

---

## Why use this plugin?

- The plugin pre-generates chunks (by default 100 locations) and stores them in memory.
- While these 100 locations are not yet filled, the plugin chooses a random location (by default between -1,000,000 and 1,000,000 blocks) and processes it as follows:

 
  1. It checks if the chunk is already generated or if it’s ocean. If either is true, it picks a new random location. This is because already-generated chunks likely have their adjacent chunks generated as well, and ocean chunks usually have adjacent ocean chunks.
  2. If the location passes this check, the plugin looks for the highest block at that location. Determining the highest block involves generating the full chunk—a more expensive operation compared to checking if a chunk exists or fetching the biome, which only generates part of the chunk.
  3. If the highest block is valid (not water, lava, etc.), the location is added to memory, and the player can be teleported there.
  4. If the block is invalid, the plugin iterates through all 16×16 blocks of the chunk. This is much cheaper than generating a new chunk, so it’s more efficient than randomly trying nearby blocks.
  5. If all blocks in the chunk are invalid, the plugin repeats this process with the 8 adjacent chunks.
  6. Only if no valid location is found in the original or adjacent chunks does it try a completely new random location, as in the initial step.


- Before a valid location is added to the buffer, the surrounding chunks are pre-generated so that when the player teleports, the chunks load instantly. The generated chunks cover the player’s view distance up to the server’s maximum, ensuring the plugin handles the worst-case scenario.

# Why such a big world?

Having a very large world ensures that the player can be easily teleported to a location that has never been explored. Why would we use a small world and have the CPU forever trying to find a valid location when there will be few locations? Using a large world allows the CPU to have less work (in return, disk space will be used more, but this shouldn't be a problem in principle since a world only occupies disk space according to the number of chunks generated; if disk space starts to become a problem because of this world, it's because the server has had too much uptime and it's time to reset the world).

# Why doesn't the world reset automatically?

The last question already answers this question quite a bit... But basically, the plugin is designed to handle huge worlds, so if the 2 million by 2 million block world has already been fully explored (default configuration), it's because the server has had years of uptime; manually resetting a world every year shouldn't be a problem.

---

## Installation

1. Download the latest version of TurboRandomTeleport from one of the following sources:
    - [GitHub Releases](https://github.com/goncalodelima/TurboRandomTeleport/releases)
    - [SpigotMC](https://www.spigotmc.org/resources/turborandomteleport.133313/)

2. Install WorldManager or Multiverse-Core

3. Place the `.jar` files in your server's `plugins` folder.

4. Configure the plugin:
   - After placing the plugin and starting your server once, you can configure it in `config.yml` and other files. The `data` folder stores locations in memory on disk, so when the server restarts, it doesn’t need to find new locations—previously found valid locations are already saved and remain unexplored.

---

### Cross-Server Support (Optional)

TurboRandomTeleport supports random teleportation between multiple servers.

To enable this feature:

1. Install the **Redis-Plugin** on every server where TurboRandomTeleport is installed.
2. Ensure that a **Redis server is running**.
3. Start the server once so the configuration files are generated.
4. In `config.yml`, configure the `cross-server-support` section:
    - Set `cross-server-support.enabled` to `true` on **all servers**.
    - Configure `cross-server-support.secondary-server` depending on the server role:
        - `false` → for servers that contain the worlds where players will be randomly teleported.
        - `true` → for servers that **do not contain RTP worlds** and only redirect players to another server.

Servers with `secondary-server: true` will forward players to the server specified in `main-server-name`.

All servers participating in cross-server teleportation **must share the same Redis instance**.

---

> ⚠️ **Important**
> Do not pre-generate chunks using plugins like Chunky or WorldBorder. These tools are designed for small worlds.  
> TurboRandomTeleport is intended for extremely large worlds, and pre-generating millions of blocks would consume terabytes of disk space and could take years to complete.
>
> The plugin already provides excellent performance, so no additional chunk pre-generation is necessary.

---

## License

TurboRandomTeleport is licensed under **GNU General Public License v3**.  
You can redistribute and/or modify it under the terms of the GPL.  
For full license details, see [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0).

---

## Contribution

- Open an **issue** to report bugs or suggest features.
- Fork the project, create a branch, and submit a **pull request**.

---

## Support / Sponsorship

If you enjoy using **TurboRandomTeleport** and want to support its development, you can sponsor me via GitHub Sponsors:

[![Sponsor @goncalodelima](https://img.shields.io/badge/Sponsor-Goncalodelima-ff69b4?style=flat&logo=github)](https://github.com/sponsors/goncalodelima)

Your support helps maintain the plugin, fix bugs, and add new features.
