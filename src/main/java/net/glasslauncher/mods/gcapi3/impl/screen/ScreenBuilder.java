package net.glasslauncher.mods.gcapi3.impl.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.glasslauncher.mods.gcapi3.api.*;
import net.glasslauncher.mods.gcapi3.impl.object.ConfigCategoryHandler;
import net.glasslauncher.mods.gcapi3.impl.object.ConfigEntryHandler;
import net.glasslauncher.mods.gcapi3.impl.object.ConfigHandlerBase;
import net.glasslauncher.mods.gcapi3.impl.screen.widget.GlassEntryListWidget;
import net.glasslauncher.mods.gcapi3.impl.screen.widget.ResetConfigWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.resource.language.TranslationStorage;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ScreenBuilder extends Screen {
    private static final String REQUIRES_RESTART = "Requires Restart";
    private static final String DONE = TranslationStorage.getInstance().get("gui.done");

    protected ScreenScrollList scrollList;
    protected HashMap<Integer, ConfigHandlerBase> buttonToEntry;
    protected final ConfigCategoryHandler baseCategory;
    protected int selectedIndex = -1;
    protected final Screen parent;
    protected final ModContainer mod;
    protected int mouseX = -1;
    protected int mouseY = -1;
    protected List<ConfigHandlerBase> configHandlerBases = new ArrayList<>();
    protected int backButtonID;
    protected List<ButtonWidget> screenButtons = new ArrayList<>();

    protected ResetConfigWidget resetConfigWidget;

    public ScreenBuilder(Screen parent, ModContainer mod, ConfigCategoryHandler baseCategory) {
        this.parent = parent;
        this.mod = mod;
        this.baseCategory = baseCategory;
        configHandlerBases.addAll(baseCategory.values.values());
            configHandlerBases = configHandlerBases.stream().filter(value -> {
                try {
                    if (value instanceof ConfigCategoryHandler) {
                        return !value.parentField.getAnnotation(ConfigCategory.class).hidden();
                    }
                    if (value instanceof ConfigEntryHandler) {
                        return !value.parentField.getAnnotation(ConfigEntry.class).hidden();
                    }
                    return false;
                } catch (Exception e) {
                    throw new RuntimeException("Error was encountered while trying to parse " + value.parentObject.getClass().getCanonicalName() + "." + value.parentField.getName(), e);
                }
            }).collect(Collectors.toCollection(ArrayList::new));
        configHandlerBases.sort((self, other) -> {
            if (other instanceof ConfigCategoryHandler) {
                return 1;
            }
            return self instanceof ConfigCategoryHandler ? -1 : 0;
        });
    }

    @Override
    public void init() {
        configHandlerBases.forEach((value) -> {
            //noinspection rawtypes
            if (value instanceof ConfigEntryHandler configEntry && configEntry.getDrawableValue() != null) {
                configEntry.value = configEntry.getDrawableValue();
            }
        });
        buttons.clear();
        screenButtons.clear();
        this.scrollList = new ScreenScrollList();
        this.buttonToEntry = new HashMap<>();
        ButtonWidget button = new ButtonWidget(backButtonID = buttons.size(),width/2-75, height-26, 150, 20, DONE);
        //noinspection unchecked
        buttons.add(button);
        screenButtons.add(button);

        configHandlerBases.forEach((value) -> {
            if (value instanceof ConfigEntryHandler<?> entryHandler) {
                entryHandler.init(this, textRenderer);
            }
            value.getDrawables().forEach(val -> {
                if (val instanceof ButtonWidget) {
                    val.setID(buttons.size());
                    buttonToEntry.put(buttons.size(), value);
                    //noinspection unchecked
                    buttons.add(val);
                }
            });
        });

        resetConfigWidget = new ResetConfigWidget(baseCategory);
    }

    @Override
    public void tick() {
        super.tick();

        configHandlerBases.forEach((value) -> value.getDrawables().forEach(HasDrawable::tick));
    }

    @Override
    protected void keyPressed(char character, int key) {
        super.keyPressed(character, key);
        configHandlerBases.forEach((value) -> {
            value.getDrawables().forEach(hasDrawable -> hasDrawable.keyPressed(character, key));
        });
    }

    @SuppressWarnings("CommentedOutCode") // I want to show code differences.
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        scrollList.render(mouseX, mouseY, delta);
        // Breaks rendering of category buttons.
        //super.render(mouseX, mouseY, delta);
        //((ButtonWidget) buttons.get(backButtonID)).render(minecraft, mouseX, mouseY);
        screenButtons.forEach(button -> button.render(minecraft, mouseX, mouseY));
        textRenderer.drawWithShadow(baseCategory.name, (width/2) - (textRenderer.getWidth(baseCategory.name)/2), 4, 16777215);
        if (baseCategory.description != null) {
            textRenderer.drawWithShadow(baseCategory.description, (width / 2) - (textRenderer.getWidth(baseCategory.description) / 2), 18, 8421504);
        }
        ArrayList<HasDrawable> drawables = new ArrayList<>();
        configHandlerBases.forEach((configHandlerBase -> drawables.addAll(configHandlerBase.getDrawables())));
        if(mouseY > 32 && mouseY < height - 33) {
            List<String> tooltip = ((ScreenAccessor) this).glass_config_api$getMouseTooltip(mouseX, mouseY, drawables);
            if (tooltip != null) {
                CharacterUtils.renderTooltip(textRenderer, tooltip, mouseX, mouseY, this);
            }
        }

        resetConfigWidget.setXYWH(width - 25, height - 10, 10, 10);
        resetConfigWidget.draw(mouseX, mouseY);
        if (((ScreenAccessor) this).glass_config_api$isMouseInBounds(resetConfigWidget.getXYWH(), mouseX, mouseY)) {
            CharacterUtils.renderTooltip(textRenderer, List.of("Reset all displayed configs to default."), mouseX, mouseY, this);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int buttonID) {
        if(mouseY < 32 || mouseY > height - 33) {
            for (ButtonWidget button : screenButtons) {
                if (button.isMouseOver(minecraft, mouseX, mouseY)) {
                    ((ScreenAccessor) this).glass_config_api$setSelectedButton(button);
                    minecraft.soundManager.method_2009("random.click", 1.0F, 1.0F);
                    buttonClicked(button);
                }
            }

            resetConfigWidget.mouseClicked(mouseX, mouseY, buttonID);
            return;
        }
        if (buttonID != 0) { // We only want left click
            return;
        }

        for (Object buttonObj : buttons) {
            ButtonWidget button = (ButtonWidget) buttonObj;
            if (button.isMouseOver(minecraft, mouseX, mouseY)) {
                ((ScreenAccessor) this).glass_config_api$setSelectedButton(button);
                minecraft.soundManager.method_2009("random.click", 1.0F, 1.0F);
                buttonClicked(button);
            }
        }
    }

    @Override
    public void onMouseEvent() {
        super.onMouseEvent();
        float dWheel = Mouse.getDWheel();
        if (Mouse.isButtonDown(0) && mouseY > 32 && mouseY < height - 33) {
            for (ConfigHandlerBase configHandlerBase : configHandlerBases) {
                configHandlerBase.getDrawables().forEach(val -> val.mouseClicked(mouseX, mouseY, 0));
            }
        }
        else if (dWheel != 0) {
            scrollList.scroll(-(dWheel/10));
        }
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == backButtonID) {
            saveToEntries();
            minecraft.setScreen(parent);
        }
        else if (mouseY >= 32 && mouseY <= height - 33) {
            if (buttonToEntry.get(button.id) instanceof ConfigEntryWithButton) {
                ((ConfigEntryWithButton) buttonToEntry.get(button.id)).onClick();
            }
            else if (buttonToEntry.get(button.id) instanceof ConfigCategoryHandler) {
                //noinspection deprecation
                ((Minecraft) FabricLoader.getInstance().getGameInstance()).setScreen(((ConfigCategoryHandler) buttonToEntry.get(button.id)).getConfigScreen(this, mod));
            }
        }
    }

    public void saveToEntries() {
        configHandlerBases.forEach((value) -> {
            if (value instanceof ConfigEntryHandler<?>) {
                //noinspection rawtypes
                ConfigEntryHandler configEntry = (ConfigEntryHandler<?>) value;
                if (configEntry.isValueValid()) {
                    configEntry.value = configEntry.getDrawableValue();
                }
                else {
                    //noinspection unchecked
                    configEntry.setDrawableValue(configEntry.value);
                }
            }
        });
    }

    public void setRequiresRestart() {
        ((ButtonWidget) buttons.get(backButtonID)).text = REQUIRES_RESTART;
        if (parent instanceof ScreenBuilder screenBuilder) {
            screenBuilder.setRequiresRestart();
        }
    }

    public class ScreenScrollList extends GlassEntryListWidget {

        public ScreenScrollList() {
            super(ScreenBuilder.this.minecraft, ScreenBuilder.this.width, ScreenBuilder.this.height, 32, ScreenBuilder.this.height - 32, 48);
            this.setDrawSelectedBox(false);
        }

        @Override
        protected int getEntryCount() {
            return configHandlerBases.size();
        }

        @Override
        protected void entryClicked(int entryIndex, boolean doLoad) {
            selectedIndex = entryIndex;
        }

        @Override
        protected boolean isSelectedEntry(int i) {
            return i == selectedIndex;
        }

        @Override
        protected void renderBackground() {}

        @Override
        protected void renderEntry(int itemId, int x, int width, int y, int i1, Tessellator arg) {
            ConfigHandlerBase configHandlerBase = configHandlerBases.get(itemId);
            ScreenBuilder.this.drawTextWithShadow(ScreenBuilder.this.textRenderer, configHandlerBase.name, x + 2, y + 1, 16777215);
            configHandlerBase.getDrawables().forEach(val -> val.setXYWH(x + 2, y + 12, width, 20));
            configHandlerBase.getDrawables().forEach(val -> val.draw(mouseX, mouseY));
            if (configHandlerBase.description != null) {
                ScreenBuilder.this.drawTextWithShadow(ScreenBuilder.this.textRenderer, configHandlerBase.description, x + 2, y + 34, 8421504);
            }
        }
    }
}
