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

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;

/**
 * LocationLinkedBuffer is a linked-list based buffer for storing {@link Location} objects,
 * typically used for random teleportation management in Minecraft.
 * <p>
 * Supports standard push/pop operations, as well as "pending" pops for asynchronous
 * teleport operations, with automatic chunk-based bookkeeping for fast invalidation
 * and controlled occupancy.
 * </p>
 */
public class LocationLinkedBuffer {

    /**
     * Node represents an entry in the linked buffer.
     * It contains a {@link Location} and pointers to previous and next nodes.
     */
    static class Node {
        Location location;
        Node prev, next;

        /**
         * Constructs a Node wrapping the specified location.
         *
         * @param location the Location to store
         */
        Node(Location location) {
            this.location = location;
        }

    }

    private Node head = null;
    private Node tail = null;

    private int occupied = 0;
    private final int capacity;

    /** Number of pending "popPending" operations awaiting confirmation */
    private int pendingAsync = 0;

    /** Mapping from chunk key to set of Nodes for fast invalidation */
    private final Map<Long, Set<Node>> chunkMap = new HashMap<>();

    /**
     * Creates a LocationLinkedBuffer with the specified capacity.
     *
     * @param capacity the maximum number of locations the buffer can hold
     */
    public LocationLinkedBuffer(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Adds a new location to the end of the buffer if capacity allows.
     *
     * @param location the Location to push
     * @param cx       chunk X coordinate
     * @param cz       chunk Z coordinate
     */
    public void push(Location location, int cx, int cz) {
        if (occupied >= capacity) return;

        Node node = new Node(location);

        // Append to the end
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        occupied++;

        // Add node to chunk map
        long chunkKey = LocationUtil.getChunkKey(cx, cz);
        chunkMap.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(node);
    }

    /**
     * Pops and removes the first location from the buffer.
     * This decrements the occupied counter and removes the node from chunk bookkeeping.
     *
     * @return the Location at the head of the buffer, or null if empty
     */
    public Location pop() {
        Node node = removeHead(false);
        return node != null ? node.location : null;
    }

    /**
     * Pops and removes the first location from the buffer for asynchronous or
     * deferred processing.
     * <p>
     * Unlike {@link #pop()}, this method does not decrement {@code occupied} immediately.
     * Instead, the caller should later call {@link #confirmPop()} to finalize the removal.
     * </p>
     *
     * @return the Location at the head of the buffer, or null if empty
     */
    public Location popPending() {
        Node node = removeHead(true);
        return node != null ? node.location : null;
    }

    /**
     * Confirms a previously "pending" pop, decrementing the occupied counter.
     * <p>
     * This should be called after a popPending() operation is successfully processed,
     * e.g., after an asynchronous teleport completes.
     * </p>
     */
    public void confirmPop() {

        if (pendingAsync > 0) {
            pendingAsync--;
            occupied--;
        }

    }

    /**
     * Invalidates all nodes within the specified chunk.
     * Removes them from the linked list and decrements the occupied counter.
     *
     * @param chunk the Chunk to invalidate
     */
    public void invalidateChunk(Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        Set<Node> nodes = chunkMap.remove(chunkKey);

        if (nodes != null) {
            for (Node node : nodes) {
                // Remove node from linked list
                if (node.prev != null) node.prev.next = node.next;
                else head = node.next;

                if (node.next != null) node.next.prev = node.prev;
                else tail = node.prev;

                occupied--;
            }
        }
    }

    /**
     * Returns a list of all locations currently in the buffer.
     *
     * @return a list of Locations in insertion order
     */
    public List<Location> getAllLocations() {

        List<Location> list = new ArrayList<>();
        Node current = head;

        while (current != null) {
            list.add(current.location);
            current = current.next;
        }

        return list;
    }

    /**
     * Returns the number of occupied slots in the buffer.
     *
     * @return the current occupancy count
     */
    public int occupied() {
        return occupied;
    }

    /**
     * Returns the maximum capacity of the buffer.
     *
     * @return the buffer capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns true if the buffer contains no locations.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return occupied == 0;
    }

    /**
     * Internal method to remove and return the head node.
     *
     * @param markPending if true, increment pendingAsync instead of decrementing occupied
     * @return the removed Node, or null if buffer is empty
     */
    private Node removeHead(boolean markPending) {
        if (head == null) return null;

        Node node = head;
        head = head.next;
        if (head != null) head.prev = null;
        else tail = null; // list became empty

        // Remove from chunkMap
        long chunkKey = LocationUtil.getChunkKey(node.location);
        Set<Node> set = chunkMap.get(chunkKey);
        if (set != null) {
            set.remove(node);
            if (set.isEmpty()) chunkMap.remove(chunkKey);
        }

        if (markPending) {
            pendingAsync++;
        } else {
            occupied--;
        }

        return node;
    }

}