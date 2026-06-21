package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GraveCompassItem extends Item implements PolymerItem {
    public static Item INSTANCE = new GraveCompassItem();

    public GraveCompassItem() {
        super(new Settings().maxCount(1));
    }

    public static ItemStack create(long graveId, boolean toVanilla) {
        var stack = new ItemStack(INSTANCE);
        stack.getOrCreateNbt().putLong("GraveId", graveId);
        stack.getNbt().putBoolean("ConvertToVanilla", toVanilla);
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayerEntity
                && ConfigManager.getConfig().interactions.useDeathCompassToOpenGui
                && stack.hasNbt() && stack.getNbt().contains("GraveId", NbtElement.LONG_TYPE)) {
            Grave grave = GraveManager.INSTANCE.getId(stack.getNbt().getLong("GraveId"));

            if (grave != null) {
                // Check if player is within 2 blocks of the grave
                var graveLoc = grave.getLocation();
                var playerWorld = serverPlayerEntity.getServerWorld();

                if (playerWorld.getRegistryKey().getValue().equals(graveLoc.world())
                        && isWithinDistance(playerWorld, graveLoc.blockPos().getX(), graveLoc.blockPos().getY(), graveLoc.blockPos().getZ(),
                        serverPlayerEntity.getBlockX(), serverPlayerEntity.getBlockY(), serverPlayerEntity.getBlockZ(), 2.0)) {
                    // Within 2 blocks: quick equip items directly
                    grave.quickEquip(serverPlayerEntity);
                } else {
                    // Far away: open GUI as before
                    grave.openUi(serverPlayerEntity, false, false);
                }
            }
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    private static boolean isWithinDistance(ServerWorld world, int graveX, int graveY, int graveZ, int playerX, int playerY, int playerZ, double distance) {
        double d = (double) (graveX - playerX);
        double e = (double) (graveY - playerY);
        double f = (double) (graveZ - playerZ);
        return d * d + e * e + f * f <= distance * distance;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.COMPASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity player && !stack.isEmpty()) {
            if (stack.hasNbt() && stack.getNbt().contains("GraveId", NbtElement.LONG_TYPE)) {
                var grave = GraveManager.INSTANCE.getId(stack.getNbt().getLong("GraveId"));

                if (grave == null) {
                    var count = stack.getCount();
                    stack.setCount(0);

                    if (stack.getNbt().getBoolean("ConvertToVanilla")) {
                        player.giveItemStack(new ItemStack(Items.COMPASS, count));
                    }
                }
            } else {
                var graveId = ((PlayerAdditions) entity).graves$lastGrave();
                if (graveId != -1) {
                    stack.getOrCreateNbt().putLong("GraveId", graveId);
                } else {
                    stack.setCount(0);
                }
            }
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        var clientStack = PolymerItem.super.getPolymerItemStack(itemStack, context, player);
        if (player != null && itemStack.hasNbt() && itemStack.getNbt().contains("GraveId", NbtElement.LONG_TYPE)) {
            var grave = GraveManager.INSTANCE.getId(itemStack.getNbt().getLong("GraveId"));
            if (grave != null) {
                clientStack.getOrCreateNbt().putString("LodestoneDimension", grave.getLocation().world().toString());
                var pos = new NbtCompound();
                pos.putInt("X", grave.getLocation().x());
                pos.putInt("Y", grave.getLocation().y());
                pos.putInt("Z", grave.getLocation().z());
                clientStack.getOrCreateNbt().put("LodestonePos", pos);
            }
        }
        clientStack.getOrCreateNbt().putBoolean("LodestoneTracked", true);
        return clientStack;
    }
}