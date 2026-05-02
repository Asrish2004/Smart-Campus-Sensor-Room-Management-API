package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

/**
 * Part 2 - Room Management
 * Base path: /api/v1/rooms
 *
 * DELETE idempotency note (Part 2 Q2):
 * Yes, DELETE is idempotent in this implementation. The first DELETE on an
 * existing room removes it and returns 200. Any subsequent DELETE for the same
 * roomId will find nothing in the map and return 404 — a different status code,
 * but no server-side state changes occur. RFC 9110 defines idempotency as
 * "the intended effect on the server is the same regardless of how many times
 * the request is sent" — once the room is gone, re-sending the request leaves
 * the system in the same state (room absent), so the operation is idempotent.
 *
 * Full vs ID list note (Part 2 Q1):
 * Returning full objects costs more bandwidth but saves the client extra GET
 * round-trips. Returning only IDs is efficient when clients need to display a
 * list and then fetch individual details lazily. For this campus API, returning
 * full objects is preferred because rooms are lightweight and facility dashboards
 * typically need all metadata at once.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /** GET /api/v1/rooms - list all rooms */
    @GET
    public Collection<Room> getAllRooms() {
        return store.rooms.values();
    }

    /** GET /api/v1/rooms/{roomId} - get a single room */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found", "roomId", roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    /** POST /api/v1/rooms - create a new room */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room id is required"))
                    .build();
        }
        if (store.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Room with id '" + room.getId() + "' already exists"))
                    .build();
        }
        store.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId} - decommission a room.
     * Business rule: cannot delete a room that still has sensors assigned.
     * Throws RoomNotEmptyException which is mapped to HTTP 409 Conflict.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found", "roomId", roomId))
                    .build();
        }

        // Safety: block deletion if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.rooms.remove(roomId);
        return Response.ok(Map.of(
                "message", "Room '" + roomId + "' has been decommissioned successfully.",
                "roomId", roomId
        )).build();
    }
}
