package com.artillerymod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

/**
 * When right-clicked on a block face this item places the corresponding
 * artillery entity into the world.
 *
 * The entity is oriented to face the same direction the player is facing
 * at the moment of placement.
 *
 * The item is consumed (stack size reduced by 1) on successful placement
 * unless the player is in Creative mode.
 */
public class ArtilleryPlacerItem extends Item {

    private final RegistryObject<? extends EntityType<?>> entityTypeSupplier;

    public ArtilleryPlacerItem(RegistryObject<? extends EntityType<?>> entityTypeSupplier,
                                Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // Optimistic; server is authoritative
        }

        Direction face   = ctx.getClickedFace();
        BlockPos  hitPos = ctx.getClickedPos();

        // Place the entity on top of the clicked face
        BlockPos spawnPos = hitPos.relative(face);
        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;

        // Check that the target position is not occupied by a solid block
        if (!level.getBlockState(spawnPos).isAir() &&
                !level.getBlockState(spawnPos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        Entity entity = entityTypeSupplier.get().create(level);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        entity.setPos(x, y, z);
        // Orient to face the same direction as the placing player
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0f;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);

        level.addFreshEntity(entity);

        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) {
            ctx.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.artillerymod.artillery_placer.tooltip"));
    }
}
