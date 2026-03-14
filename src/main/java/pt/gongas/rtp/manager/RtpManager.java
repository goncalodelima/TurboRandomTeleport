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

package pt.gongas.rtp.manager;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pt.gongas.rtp.store.cooldown.CooldownStore;
import pt.gongas.rtp.store.lock.TeleportLockStore;
import pt.gongas.rtp.util.LocationLinkedBuffer;
import pt.gongas.rtp.util.config.Configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RtpManager {

    private final Logger logger;

    private final CooldownStore cooldownStore;

    private final TeleportLockStore teleportLockStore;

    private final Component teleportMessage;

    private final PotionEffect resistanceEffect = new PotionEffect(PotionEffectType.RESISTANCE, 20 * 5, 255);

    private final PotionEffect weaknessEffect = new PotionEffect(PotionEffectType.WEAKNESS, 20 * 5, 255);

    private static final Sound TELEPORT_SOUND = Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.UI, 1f, 1f);

    public RtpManager(Logger logger, Configuration lang, CooldownStore cooldownStore, TeleportLockStore teleportLockStore) {
        this.logger = logger;
        this.cooldownStore = cooldownStore;
        this.teleportLockStore = teleportLockStore;
        this.teleportMessage = MiniMessage.miniMessage().deserialize(lang.getString("teleport-success", "<green>You have been successfully teleported! You are invincible for 5 seconds..."));
    }

    public void teleportPlayer(Player player, LocationLinkedBuffer locationLinkedBuffer) {

        // Pop a location from the buffer in a "pending" state.
        // This ensures the location is temporarily reserved and not taken by another teleport
        // while the async teleport is in progress. The occupied counter will only be decremented
        // if the teleport succeeds (via confirmPop()), otherwise the location is returned to the buffer.
        Location location = locationLinkedBuffer.popPending();

        if (location == null) {
            teleportLockStore.remove(player.getUniqueId());
            return;
        }

        cooldownStore.put(player.getUniqueId(), System.currentTimeMillis()).thenAccept(cooldownSuccess -> {

            if (!cooldownSuccess) {
                teleportLockStore.remove(player.getUniqueId());
                return;
            }

            player.teleportAsync(location.add(0.5, 0, 0.5))
                    .whenComplete((success, throwable) -> {

                        try {

                            if (throwable != null) {
                                locationLinkedBuffer.failPop();
                                locationLinkedBuffer.push(location, location.getBlockX() >> 4, location.getBlockZ() >> 4);
                                logger.log(Level.SEVERE, "An error occurred while trying to teleport to " + location, throwable);
                                return;
                            }

                            if (Boolean.TRUE.equals(success)) {

                                locationLinkedBuffer.confirmPop();

                                player.addPotionEffect(resistanceEffect);
                                player.addPotionEffect(weaknessEffect);

                                player.playSound(TELEPORT_SOUND, Sound.Emitter.self());
                                player.sendMessage(teleportMessage);

                            } else {
                                locationLinkedBuffer.failPop();
                                locationLinkedBuffer.push(location, location.getBlockX() >> 4, location.getBlockZ() >> 4);
                            }

                        } catch (Throwable t) {
                            logger.log(Level.SEVERE, "Error after teleport completion for " + location, t);
                        } finally {
                            teleportLockStore.remove(player.getUniqueId());
                        }

                    });

        });

    }

}
