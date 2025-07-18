package net.glasslauncher.mods.gcapi3.impl.object.entry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.api.ConfigEntry;
import net.glasslauncher.mods.gcapi3.api.ConfigEntryWithButton;
import net.glasslauncher.mods.gcapi3.api.HasDrawable;
import net.glasslauncher.mods.gcapi3.impl.object.ConfigEntryHandler;
import net.glasslauncher.mods.gcapi3.impl.screen.BaseListScreenBuilder;
import net.glasslauncher.mods.gcapi3.impl.screen.ScreenBuilder;
import net.glasslauncher.mods.gcapi3.impl.screen.widget.FancyButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseListConfigEntryHandler<T> extends ConfigEntryHandler<T[]> implements ConfigEntryWithButton {
    @Environment(EnvType.CLIENT)
    private BaseListScreenBuilder<T> listScreen;
    @Environment(EnvType.CLIENT)
    private FancyButtonWidget button;
    protected Runnable textUpdatedListener = () -> {
        if (configEntry.requiresRestart() && parent instanceof ScreenBuilder screenBuilder) {
            screenBuilder.setRequiresRestart();
        }
    };

    public BaseListConfigEntryHandler(String id, ConfigEntry configEntry, Field parentField, Object parentObject, boolean multiplayerSynced, T[] value, T[] defaultValue) {
        super(id, configEntry, parentField, parentObject, multiplayerSynced, value, defaultValue);
    }

    @Override
    public void init(Screen parent, TextRenderer textRenderer) {
        super.init(parent, textRenderer);
        button = new FancyButtonWidget(10, 0, 0, 0, 0, "Open List... (" + value.length + " values)");
        drawableList.add(button);
        listScreen = createListScreen(parent);
        button.active = !multiplayerLoaded;
    }

    @Environment(EnvType.CLIENT)
    public abstract BaseListScreenBuilder<T> createListScreen(Screen parent);

    public abstract T strToVal(String str);

    @Override
    public T[] getDrawableValue() {
        if (listScreen == null) {
            return null;
        }
        List<T> list = new ArrayList<>();
        listScreen.textFieldWidgets.forEach((val) -> {
            if (val.isValueValid()) {
                list.add(strToVal(val.getText()));
            }
        });

        return list.toArray(getTypedArray());
    }

    public abstract T[] getTypedArray();

    @Override
    public boolean isValueValid() {
        if (value.length > configEntry.maxArrayLength()) {
            return false;
        }
        if(value.length < configEntry.minArrayLength()) {
            return false;
        }
        return listContentsValid();
    }

    public boolean listContentsValid() {
        return Arrays.stream(value).noneMatch(aValue -> textValidator.apply(aValue.toString()) != null);
    }

    @Override
    public void setDrawableValue(T[] value) {
        listScreen.setValues(value);
    }

    @Override
    public @NotNull List<HasDrawable> getDrawables() {
        return drawableList;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void onClick() {
        //noinspection deprecation
        ((net.minecraft.client.Minecraft) FabricLoader.getInstance().getGameInstance()).setScreen(listScreen);
    }

    @Override
    public void reset(Object defaultValue) throws IllegalAccessException { // !!OVERRIDE THIS AND DO A DEEP CLONE IF YOU'RE USING SOMETHING THAT ISN'T A PRIMITIVE/SINGLETON/OTHERWISE UNIQUE VALUE!!
        //noinspection unchecked
        value = ((T[]) defaultValue).clone();
        saveToField();
    }
}

