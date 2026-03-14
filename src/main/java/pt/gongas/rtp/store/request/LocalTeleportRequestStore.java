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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LocalTeleportRequestStore implements TeleportRequestStore {

    private final Map<UUID, String> requests = new HashMap<>();

    @Override
    public CompletableFuture<Boolean> put(UUID uuid, String worldType) {
        requests.put(uuid, worldType);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<String> get(UUID uuid) {
        return CompletableFuture.completedFuture(requests.get(uuid));
    }

    @Override
    public CompletableFuture<String> remove(UUID uuid) {
        return CompletableFuture.completedFuture(requests.remove(uuid));
    }

    @Override
    public CompletableFuture<Boolean> containsKey(UUID uuid) {
        return CompletableFuture.completedFuture(requests.containsKey(uuid));
    }

}