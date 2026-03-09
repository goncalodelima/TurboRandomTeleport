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

package pt.gongas.rtp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pt.gongas.rtp.util.LocationLinkedBuffer;

public enum WorldType {

    OVERWORLD("resources_world"),
    NETHER("resources_nether_world"),
    END("resources_end_world");

    private final String worldName;

    private LocationLinkedBuffer linkedBuffer;

    WorldType(String worldName) {
        this.worldName = worldName;
    }

    public @NotNull String getWorldName() {
        return worldName;
    }

    /**
     * Returns the {@link LocationLinkedBuffer} for this {@link WorldType}.
     * <p>
     * This is guaranteed to be non-null (@NotNull) **only if**
     * {@link #setLinkedBuffer(LocationLinkedBuffer)} has been called beforehand.
     */
    public @NotNull LocationLinkedBuffer getLinkedBuffer() {
        return linkedBuffer;
    }

    public void setLinkedBuffer(@NotNull LocationLinkedBuffer linkedBuffer) {
        this.linkedBuffer = linkedBuffer;
    }

    public static @Nullable WorldType getByWorldName(@NotNull String worldName) {

        return switch (worldName) {
            case "resources_world" -> WorldType.OVERWORLD;
            case "resources_nether_world" -> WorldType.NETHER;
            case "resources_end_world" -> WorldType.END;
            default -> null;
        };

    }

}
