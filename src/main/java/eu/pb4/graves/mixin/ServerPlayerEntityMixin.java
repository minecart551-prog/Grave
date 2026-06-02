package eu.pb4.graves.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.graves.other.GraveUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PlayerAdditions {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private long graves$location = -1;

    @Unique
    private boolean graves$hasCompass = false;

    @Unique
    private boolean graves$isInvulnerable = false;

    @Unique
    private boolean graves$diedOfSuffocation = false;

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void graves$loadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("LastGraveId", NbtElement.LONG_TYPE)) {
            this.graves$location = nbt.getLong("LastGraveId");
        }

        this.graves$hasCompass = nbt.getBoolean("HasGraveCompass");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void graves$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.graves$location != -1) {
            nbt.putLong("LastGraveId", this.graves$location);
        }

         nbt.putBoolean("HasGraveCompass", this.graves$hasCompass);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void graves$copyDate(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.graves$hasCompass = ((ServerPlayerEntityMixin) (Object) oldPlayer).graves$hasCompass;
        this.graves$location = ((PlayerAdditions) oldPlayer).graves$lastGrave();
        
        // Transfer blocked items from old player to new player on death
        if (!alive) {
            var config = ConfigManager.getConfig();
            var oldInventory = oldPlayer.getInventory();
            var newInventory = this.getInventory();

            // On suffocation death: copy all items (no grave created)
            // The LivingEntityMixin cancels the entire drop() event for suffocation,
            // so items are still in the old player's inventory.
            // We check by looking if the old player still has items in slots
            // (since the drop was cancelled, items shouldn't be cleared)
            boolean hasItemsInOldInv = false;
            for (int i = 0; i < oldInventory.main.size() && !hasItemsInOldInv; i++) {
                if (!oldInventory.main.get(i).isEmpty()) {
                    hasItemsInOldInv = true;
                }
            }
            if (hasItemsInOldInv && ((PlayerAdditions) oldPlayer).graves$diedOfSuffocation()) {
                GraveUtils.copyAllItems(oldPlayer, (ServerPlayerEntity) (Object) this);
                return;
            }
            
            // Copy blocked items from main inventory
            for (int i = 0; i < oldInventory.main.size(); i++) {
                ItemStack stack = oldInventory.main.get(i);
                if (!stack.isEmpty() && GraveUtils.isBlockedItem(stack)) {
                    // Add to new player's main inventory
                    newInventory.main.set(i, stack.copy());
                }
            }
            
            // Copy blocked items from armor
            for (int i = 0; i < oldInventory.armor.size(); i++) {
                ItemStack stack = oldInventory.armor.get(i);
                if (!stack.isEmpty() && GraveUtils.isBlockedItem(stack)) {
                    // Add to new player's armor
                    newInventory.armor.set(i, stack.copy());
                }
            }
            
            // Copy blocked items from offhand
            ItemStack oldOffhand = oldInventory.offHand.get(0);
            if (!oldOffhand.isEmpty() && GraveUtils.isBlockedItem(oldOffhand)) {
                newInventory.offHand.set(0, oldOffhand.copy());
            }

            // In keep-inventory zones: copy non-excepted items that were left in inventory slots
            if (config.keepInventoryZones.enabled && !config.keepInventoryZones.zones.isEmpty()) {
                // Only attempt copy if old player died within a zone - check their position
                // Since copyFrom happens after death and before respawn, check old player
                var zonePos = oldPlayer.getBlockPos();
                boolean inZone = false;
                for (var zone : config.keepInventoryZones.zones) {
                    if (zone.contains(zonePos.getX(), zonePos.getY(), zonePos.getZ())) {
                        inZone = true;
                        break;
                    }
                }

                if (inZone && !config.keepInventoryZones.exceptItems.isEmpty()) {
                    // Copy non-excepted items from main inventory
                    for (int i = 0; i < oldInventory.main.size(); i++) {
                        ItemStack stack = oldInventory.main.get(i);
                        if (!stack.isEmpty() && !config.keepInventoryZones.exceptItems.contains(Registries.ITEM.getId(stack.getItem()))
                                && !GraveUtils.isBlockedItem(stack) && !EnchantmentHelper.hasVanishingCurse(stack)) {
                            if (newInventory.main.get(i).isEmpty()) {
                                newInventory.main.set(i, stack.copy());
                            }
                        }
                    }

                    // Copy non-excepted items from armor
                    for (int i = 0; i < oldInventory.armor.size(); i++) {
                        ItemStack stack = oldInventory.armor.get(i);
                        if (!stack.isEmpty() && !config.keepInventoryZones.exceptItems.contains(Registries.ITEM.getId(stack.getItem()))
                                && !GraveUtils.isBlockedItem(stack) && !EnchantmentHelper.hasVanishingCurse(stack)) {
                            if (newInventory.armor.get(i).isEmpty()) {
                                newInventory.armor.set(i, stack.copy());
                            }
                        }
                    }

                    // Copy non-excepted items from offhand
                    ItemStack offhandStack = oldInventory.offHand.get(0);
                    if (!offhandStack.isEmpty() && !config.keepInventoryZones.exceptItems.contains(Registries.ITEM.getId(offhandStack.getItem()))
                            && !GraveUtils.isBlockedItem(offhandStack) && !EnchantmentHelper.hasVanishingCurse(offhandStack)) {
                        if (newInventory.offHand.get(0).isEmpty()) {
                            newInventory.offHand.set(0, offhandStack.copy());
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void graves$setupThings(DamageSource source, CallbackInfo ci) {
        this.graves$location = -1;
        this.graves$hasCompass = false;
    }

    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void graves$onSpawn(CallbackInfo ci) {
        if (this.graves$location != -1 && !this.graves$hasCompass && ConfigManager.getConfig().interactions.giveGraveCompass) {
            this.getInventory().offerOrDrop(GraveCompassItem.create(this.graves$location, false));
            this.graves$hasCompass = true;
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void graves$isInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
         if (this.graves$isInvulnerable) {
             cir.setReturnValue(true);
         }
    }

    @Override
    public Text graves$lastDeathCause() {
        return null;
    }

    @Override
    public long graves$lastGrave() {
        return this.graves$location;
    }

    @Override
    public void graves$setLastGrave(long id) {
        this.graves$location = id;
    }

    @Override
    public void graves$setInvulnerable(boolean value) {
        this.graves$isInvulnerable = value;
    }

    @Override
    public boolean graves$diedOfSuffocation() {
        return this.graves$diedOfSuffocation;
    }

    @Override
    public void graves$setDiedOfSuffocation(boolean diedOfSuffocation) {
        this.graves$diedOfSuffocation = diedOfSuffocation;
    }
}
