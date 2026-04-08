package com.artillerymod.entity;

/**
 * State machine for the artillery breech mechanism.
 *
 * Cycle:
 *   EMPTY → (player loads ammo) → LOADING → LOADED → (player fires) → FIRING → COOLDOWN → EMPTY
 *
 * LOADING represents the physical loading delay (ramming the shell home, closing the breech).
 * FIRING is a single-tick visual state used to trigger recoil effects.
 * COOLDOWN is the post-fire delay before the breech can accept another round
 * (smoke clearing, case extraction).
 */
public enum BreechState {

    /** No round in the chamber; ready to accept ammunition. */
    EMPTY(0),

    /** A round has been inserted; the breech is closing (timed delay). */
    LOADING(1),

    /** Round chambered, breech closed, weapon ready to fire. */
    LOADED(2),

    /** Weapon has just fired; single-tick state for recoil/particle effects. */
    FIRING(3),

    /** Post-fire cooling / case extraction delay before next loading. */
    COOLDOWN(4);

    private final int id;

    BreechState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static BreechState byId(int id) {
        for (BreechState s : values()) {
            if (s.id == id) return s;
        }
        return EMPTY;
    }
}
