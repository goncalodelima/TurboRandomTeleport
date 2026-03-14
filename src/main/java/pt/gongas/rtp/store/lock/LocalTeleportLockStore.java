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

package pt.gongas.rtp.store.lock;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LocalTeleportLockStore implements TeleportLockStore {

    private final Set<UUID> lockedPlayers = new HashSet<>();

    @Override
    public CompletableFuture<Boolean> add(UUID uuid) {
        return CompletableFuture.completedFuture(lockedPlayers.add(uuid));
    }

    @Override
    public CompletableFuture<Boolean> contains(UUID uuid) {
        return CompletableFuture.completedFuture(lockedPlayers.contains(uuid));
    }

    @Override
    public CompletableFuture<Boolean> remove(UUID uuid) {
        return CompletableFuture.completedFuture(lockedPlayers.remove(uuid));
    }

}