package com.artillerymod.network;

import com.artillerymod.entity.AbstractArtilleryEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client → Server packet sent when the player presses the hitch keybind.
 *
 * Server logic (toggle):
 *   1. Find the nearest artillery entity within SEARCH_RADIUS of the player.
 *   2. If that gun is already towed → detach it (unhitch).
 *   3. If not towed → find the nearest horse within HORSE_RADIUS of the gun
 *      and attach the gun to it.
 *
 * Historical note:
 *   WW2 horse-drawn artillery (Pak 36, 45 mm M1937) was connected via the
 *   gun's trail spade to a limber pole.  A single press toggles the limber
 *   connection, reflecting the quick-hitch/unhitch drill used by gun crews.
 */
public final class HitchRequestPacket {

    /** Max distance (blocks) from player to gun for the action to trigger. */
    private static final double GUN_SEARCH_RADIUS  = 8.0;
    /** Max distance (blocks) from gun to horse when hitching. */
    private static final double HORSE_SEARCH_RADIUS = 10.0;

    // ── Codec ─────────────────────────────────────────────────────────────────

    public HitchRequestPacket() {}

    public static void encode(HitchRequestPacket pkt, FriendlyByteBuf buf) {
        // No payload needed – it's a pure toggle request
    }

    public static HitchRequestPacket decode(FriendlyByteBuf buf) {
        return new HitchRequestPacket();
    }

    // ── Server handler ────────────────────────────────────────────────────────

    public static void handle(HitchRequestPacket pkt,
                               Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // ── 1. Find nearest artillery within reach ─────────────────────
            List<AbstractArtilleryEntity> guns = player.level()
                    .getEntitiesOfClass(AbstractArtilleryEntity.class,
                            player.getBoundingBox().inflate(GUN_SEARCH_RADIUS));

            if (guns.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§7[Artillery] §cNo gun within range."), true);
                return;
            }
            guns.sort(Comparator.comparingDouble(g -> g.distanceToSqr(player)));
            AbstractArtilleryEntity gun = guns.get(0);

            // ── 2. Toggle: unhitch if already towed ────────────────────────
            if (gun.getTowHorseId() != -1) {
                gun.detachFromHorse();
                player.displayClientMessage(
                        Component.literal("§7[Artillery] §eGun unhitched from horse."), true);
                return;
            }

            // ── 3. Find nearest horse to hitch to ─────────────────────────
            List<AbstractHorse> horses = player.level()
                    .getEntitiesOfClass(AbstractHorse.class,
                            gun.getBoundingBox().inflate(HORSE_SEARCH_RADIUS));

            if (horses.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§7[Artillery] §cNo horse nearby to hitch to."), true);
                return;
            }
            horses.sort(Comparator.comparingDouble(h -> h.distanceToSqr(gun)));
            AbstractHorse horse = horses.get(0);

            gun.attachToHorse(horse.getId());
            player.displayClientMessage(
                    Component.literal("§7[Artillery] §aGun hitched — horse will drag it."), true);
        });
        ctx.setPacketHandled(true);
    }
}
