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

package pt.gongas.rtp.util;

import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.rtp.RtpPlugin;

import java.util.HashMap;
import java.util.Map;

public class ExpiringMap<K, V> {

    private final Map<K, Entry<V>> map = new HashMap<>();
    private final long ttlTicks;

    private static class Entry<V> {

        V value;
        long expireAt;

        Entry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

    }

    public ExpiringMap(RtpPlugin plugin, int ttlTicks) {

        this.ttlTicks = ttlTicks;

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimer(plugin, ttlTicks, ttlTicks);

    }

    public void put(K key, V value) {
        long ttlMillis = ttlTicks * 50L;
        map.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    public V get(K key) {

        Entry<V> entry = map.get(key);

        if (entry == null) {
            return null;
        }

        if (System.currentTimeMillis() > entry.expireAt) {
            map.remove(key);
            return null;
        }

        return entry.value;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

}