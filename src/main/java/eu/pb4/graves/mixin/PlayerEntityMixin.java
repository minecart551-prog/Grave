package eu.pb4.graves.mixin;

import eu.pb4.graves.other.GraveUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dropInventory", cancellable = true, at = @At("HEAD"))
    private void onDropInventory(CallbackInfo ci) {
        // Replicate dropInventory but skip blocked items
        PlayerEntity player = (PlayerEntity) (Object) this;
        var inventory = player.getInventory();
        
        // Drop main inventory
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty()) {
                // Skip blocked items - don't drop them
                if (GraveUtils.isBlockedItem(stack)) {
                    continue; // Skip this item entirely
                }
                
                // Drop non-blocked items normally
                if (!EnchantmentHelper.hasVanishingCurse(stack)) {
                    player.dropItem(stack, true, false);
                }
                inventory.main.set(i, ItemStack.EMPTY);
            }
        }
        
        // Drop armor
        for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = inventory.armor.get(i);
            if (!stack.isEmpty()) {
                if (GraveUtils.isBlockedItem(stack)) {
                    continue; // Skip blocked items
                }
                
                if (!EnchantmentHelper.hasVanishingCurse(stack)) {
                    player.dropItem(stack, true, false);
                }
                inventory.armor.set(i, ItemStack.EMPTY);
            }
        }
        
        // Drop offhand
        ItemStack offhandStack = inventory.offHand.get(0);
        if (!offhandStack.isEmpty()) {
            if (GraveUtils.isBlockedItem(offhandStack)) {
                // Skip blocked items - don't drop them
            } else {
                if (!EnchantmentHelper.hasVanishingCurse(offhandStack)) {
                    player.dropItem(offhandStack, true, false);
                }
                inventory.offHand.set(0, ItemStack.EMPTY);
            }
        }
        
        // Cancel the default dropInventory() to use our custom version
        ci.cancel();
    }
}
