package net.glasslauncher.mods.gcapi3.mixin.client;

import com.google.common.collect.ImmutableMap;
import io.github.prospector.modmenu.ModMenu;
import net.glasslauncher.mods.gcapi3.impl.GCCore;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.function.*;

@Mixin(ModMenu.class)
public class ModMenuMixin {

    @Inject(method = "onInitializeClient", at = @At(target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;", value = "INVOKE", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private void hijackConfigScreens(CallbackInfo ci, ImmutableMap.Builder<String, Function<Screen, ? extends Screen>> builder) {
        //noinspection deprecation
        GCCore.log("Adding config screens to ModMenu...");
        Map<String, Function<Screen, ? extends Screen>> map = new HashMap<>();
        //noinspection deprecation
        GCCore.MOD_CONFIGS.forEach((key, value) -> {
            String namespace = key.split(":")[0];
            if (!map.containsKey(namespace)) {
                map.remove(namespace);
                map.put(namespace, (parent) -> value.configCategoryHandler().getConfigScreen(parent, value.modContainer()));
            }
        });
        builder.putAll(map);
    }

}
