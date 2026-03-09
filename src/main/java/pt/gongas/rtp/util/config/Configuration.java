/*
 *
 *  * This file is part of TurboRandomTeleport - https://github.com/goncalodelima/TurboRandomTeleport
 *  * Copyright (c) 2026 goncalodelima and contributors
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package pt.gongas.rtp.util.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Configuration extends YamlConfiguration {

    private final File file;
    private final JavaPlugin plugin;
    private final String name;
    private final String directory;

    public Configuration(JavaPlugin plugin, String directory, String name) {
        this.directory = directory;
        file = new File((this.plugin = plugin).getDataFolder() + File.separator + this.directory, this.name = name);
    }

    public void reloadConfig() {
        try {
            load(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Exception when loading configuration file!", e);
        }
    }

    public void saveConfig() {
        try {
            save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Exception when saving configuration file!", e);
        }
    }

    public void saveDefaultConfig() {
        plugin.saveResource(this.directory + File.separator + name, false);
        reloadConfig();
    }

}
