package com.artillerymod.item;

import com.artillerymod.entity.AmmoType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a single ammunition type item.
 *
 * The item carries its {@link AmmoType} as a final field so compatibility
 * checks are pure enum comparisons – no NBT required.
 *
 * To load ammo: Sneak + Right-click on an artillery entity while holding
 * a stack of this item.  One round is consumed per load cycle.
 */
public class AmmoItem extends Item {

    private final AmmoType ammoType;

    public AmmoItem(AmmoType ammoType, Properties properties) {
        super(properties);
        this.ammoType = ammoType;
    }

    public AmmoType getAmmoType() {
        return ammoType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        // Calibre line
        tooltipComponents.add(Component.translatable(
                "item.artillerymod." + ammoType.getRegistryName() + ".tooltip_calibre"));
        // Type line
        tooltipComponents.add(Component.translatable(
                "item.artillerymod." + ammoType.getRegistryName() + ".tooltip_type"));
    }
}
