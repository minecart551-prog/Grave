package eu.pb4.graves.mixin.client;

import eu.pb4.graves.network.GraveClientState;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
    @Unique
    private boolean graves$shouldDelayRespawn() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world != null) {
            return !client.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
        }
        return true;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void graves$hideRespawnButton(CallbackInfo ci) {
        GraveClientState.reset();
        if (!graves$shouldDelayRespawn()) {
            return;
        }
        DeathScreenAccessor accessor = (DeathScreenAccessor) this;
        var buttons = accessor.graves$getButtons();
        if (buttons != null && !buttons.isEmpty()) {
            buttons.get(0).visible = false;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void graves$showRespawnAfterDelay(CallbackInfo ci) {
        if (!graves$shouldDelayRespawn()) {
            return;
        }
        DeathScreenAccessor accessor = (DeathScreenAccessor) this;
        var buttons = accessor.graves$getButtons();
        if (buttons != null && !buttons.isEmpty()) {
            var respawnButton = buttons.get(0);
            if (!respawnButton.visible && GraveClientState.graveCreated) {
                respawnButton.visible = true;
            }
        }
    }
}
