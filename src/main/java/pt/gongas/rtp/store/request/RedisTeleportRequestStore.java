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

package pt.gongas.rtp.store.request;

import org.redisson.api.RMapCache;
import pt.gongas.redis.redis.RedisManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RedisTeleportRequestStore implements TeleportRequestStore {

    private final RMapCache<UUID, String> requests;

    private static final int REQUESTS_TTL_SECONDS = 60;

    public RedisTeleportRequestStore() {
        this.requests = RedisManager.getClient().getMapCache("turbo-rtp:requests");
    }

    @Override
    public CompletableFuture<Boolean> put(UUID uuid, String worldType) {
        return requests.putIfAbsentAsync(uuid, worldType, REQUESTS_TTL_SECONDS, TimeUnit.SECONDS)
                .toCompletableFuture()
                .thenApply(Objects::isNull);
    }

    @Override
    public CompletableFuture<String> get(UUID uuid) {
        return requests.getAsync(uuid).toCompletableFuture();
    }

    @Override
    public CompletableFuture<String> remove(UUID uuid) {
        return requests.removeAsync(uuid).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Boolean> containsKey(UUID uuid) {
        return requests.containsKeyAsync(uuid).toCompletableFuture();
    }
}