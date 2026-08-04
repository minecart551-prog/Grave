package eu.pb4.graves.mixin.client;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor("buttons")
    List<ButtonWidget> graves$getButtons();

    @Accessor("ticksSinceDeath")
    int graves$getTicksSinceDeath();
}
