package net.glasslauncher.mods.gcapi3.impl.screen.widget;

import lombok.Getter;
import lombok.Setter;
import net.glasslauncher.mods.gcapi3.api.CharacterUtils;
import net.glasslauncher.mods.gcapi3.api.HasDrawable;
import net.glasslauncher.mods.gcapi3.api.HasToolTip;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Basically a modified Textbox from r1.2.5, but modified for gcapi's use case.
 */
public class ExtensibleTextFieldWidget extends DrawContext implements HasDrawable, HasToolTip {

    private static final int serverSyncedBorder = CharacterUtils.getIntFromColour(new Color(255, 202, 0, 255));
    private static final int serverSyncedText = CharacterUtils.getIntFromColour(new Color(170, 139, 21, 255));
    private final TextRenderer textRenderer;
    public int x;
    public int y;
    public int width;
    public int height;
    @Getter private String text = "";
    @Getter private int maxLength = 32;
    private int focusedTicks;
    private boolean shouldDrawBackground = true;
    @Getter @Setter private boolean enabled = true;
    private boolean selected = false;
    @SuppressWarnings("FieldMayBeFinal")
    private boolean focusable = true;
    private int cursorPosition = 0;
    @Getter private int cursorMax = 0;
    @Getter private int cursorMin = 0;
    public int selectedTextColour = 14737632;
    public int deselectedTextColour = 7368816;
    public int errorBorderColour = CharacterUtils.getIntFromColour(new Color(200, 50, 50));

    private boolean doRenderUpdate = true;
    private Function<String, List<String>> contentsValidator;
    @Setter private Runnable textUpdatedListener;

    public ExtensibleTextFieldWidget(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
    }

    public boolean isValueValid() {
        if (contentsValidator != null) {
            return contentsValidator.apply(getText()) == null;
        }
        return true;
    }

    @Override
    public void tick() {
        ++this.focusedTicks;
    }

    public void setText(String string) {
        if (string.length() > this.maxLength) {
            this.text = string.substring(0, this.maxLength);
        } else {
            this.text = string;
        }

        this.onTextChanged();
    }

    public String getSelectedText() {
        int var1 = Math.min(this.cursorMax, this.cursorMin);
        int var2 = Math.max(this.cursorMax, this.cursorMin);
        return this.text.substring(var1, var2);
    }

    /**
     * Replaces highlighted text.
     */
    public void addText(String string) {
        String var2 = "";
        String var3 = CharacterUtils.stripInvalidChars(string);
        int var4 = Math.min(this.cursorMax, this.cursorMin);
        int var5 = Math.max(this.cursorMax, this.cursorMin);
        int var6 = this.maxLength - this.text.length() - (var4 - this.cursorMin);
        if (!this.text.isEmpty()) {
            var2 = var2 + this.text.substring(0, var4);
        }

        int var8;
        if (var6 < var3.length()) {
            var2 = var2 + var3.substring(0, var6);
            var8 = var6;
        } else {
            var2 = var2 + var3;
            var8 = var3.length();
        }

        if (!this.text.isEmpty() && var5 < this.text.length()) {
            var2 = var2 + this.text.substring(var5);
        }

        this.text = var2;
        this.updateOffsetCursorMax(var4 - this.cursorMin + var8);
        if (textUpdatedListener != null) {
            textUpdatedListener.run();
        }
    }

    public void removeWord(int countAndDirection) {
        if (!this.text.isEmpty()) {
            if (this.cursorMin != this.cursorMax) {
                this.addText("");
            } else {
                this.removeRelativeToCursor(this.getWords(countAndDirection) - this.cursorMax);
            }
        }
    }

    public void removeRelativeToCursor(int countAndDirection) {
        if (!this.text.isEmpty()) {
            if (this.cursorMin != this.cursorMax) {
                this.addText("");
            } else {
                boolean backwards = countAndDirection < 0;
                int amountToTryForwards = backwards ? this.cursorMax + countAndDirection : this.cursorMax;
                int amountToTryBackwards = backwards ? this.cursorMax : this.cursorMax + countAndDirection;
                String foundCharacters = "";
                if (amountToTryForwards >= 0) {
                    foundCharacters = this.text.substring(0, amountToTryForwards);
                }

                if (amountToTryBackwards < this.text.length()) {
                    foundCharacters = foundCharacters + this.text.substring(amountToTryBackwards);
                }

                this.text = foundCharacters;
                if (backwards) {
                    this.updateOffsetCursorMax(countAndDirection);
                }

            }
        }
    }

    public int getWords(int directionAndCount) {
        return this.getWords(directionAndCount, this.getCursorMax());
    }

    public int getWords(int directionAndCount, int maximumSize) {
        int endOfWordsIndex = maximumSize;
        boolean backwards = directionAndCount < 0;
        int maximumWords = Math.abs(directionAndCount);

        for(int i = 0; i < maximumWords; ++i) {
            if (!backwards) {
                int var7 = this.text.length();
                endOfWordsIndex = this.text.indexOf(32, endOfWordsIndex);
                if (endOfWordsIndex == -1) {
                    endOfWordsIndex = var7;
                } else {
                    while(endOfWordsIndex < var7 && this.text.charAt(endOfWordsIndex) == ' ') {
                        ++endOfWordsIndex;
                    }
                }
            } else {
                while(endOfWordsIndex > 0 && this.text.charAt(endOfWordsIndex - 1) == ' ') {
                    --endOfWordsIndex;
                }

                while(endOfWordsIndex > 0 && this.text.charAt(endOfWordsIndex - 1) != ' ') {
                    --endOfWordsIndex;
                }
            }
        }

        return endOfWordsIndex;
    }

    public void updateOffsetCursorMax(int cursorMax) {
        this.updateCursorMax(this.cursorMin + cursorMax);
    }

    public void updateCursorMax(int cursorMax) {
        this.cursorMax = cursorMax;
        int var2 = this.text.length();
        if (this.cursorMax < 0) {
            this.cursorMax = 0;
        }

        if (this.cursorMax > var2) {
            this.cursorMax = var2;
        }

        this.updateCursorPosition(this.cursorMax);
    }

    public void updateCursorMax() {
        this.updateCursorMax(0);
    }

    public void onTextChanged() {
        this.updateCursorMax(this.text.length());
    }

    @Override
    public void keyPressed(char c, int i) {
        if (this.focusable && this.selected) {
            switch(c) {
                case '\u0001':
                    this.onTextChanged();
                    this.updateCursorPosition(0);
                    return;
                case '\u0003':
                    CharacterUtils.setClipboardText(this.getSelectedText());
                    return;
                case '\u0016':
                    this.addText(CharacterUtils.getClipboardText());
                    return;
                case '\u0018':
                    CharacterUtils.setClipboardText(this.getSelectedText());
                    this.addText("");
                    return;
                default:
                    switch(i) {
                        case Keyboard.KEY_BACK:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                                this.removeWord(-1);
                            } else {
                                this.removeRelativeToCursor(-1);
                            }

                            return;
                        case Keyboard.KEY_HOME:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                                this.updateCursorPosition(0);
                            } else {
                                this.updateCursorMax();
                            }

                            return;
                        case Keyboard.KEY_LEFT:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                    this.updateCursorPosition(this.getWords(-1, this.getCursorMin()));
                                } else {
                                    this.updateCursorPosition(this.getCursorMin() - 1);
                                }
                            } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                this.updateCursorMax(this.getWords(-1));
                            } else {
                                this.updateOffsetCursorMax(-1);
                            }

                            return;
                        case Keyboard.KEY_RIGHT:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                    this.updateCursorPosition(this.getWords(1, this.getCursorMin()));
                                } else {
                                    this.updateCursorPosition(this.getCursorMin() + 1);
                                }
                            } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                this.updateCursorMax(this.getWords(1));
                            } else {
                                this.updateOffsetCursorMax(1);
                            }

                            return;
                        case Keyboard.KEY_END:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                                this.updateCursorPosition(this.text.length());
                            } else {
                                this.onTextChanged();
                            }

                            return;
                        case Keyboard.KEY_DELETE:
                            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                this.removeWord(1);
                            } else {
                                this.removeRelativeToCursor(1);
                            }

                            return;
                        default:
                            if (CharacterUtils.isCharacterValid(c)) {
                                this.addText(Character.toString(c));
                            }
                    }
            }
        }
    }

    @Override
    public void setID(int id) {

    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean isMouseHovering = mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        if (this.enabled) {
            this.setSelected(this.focusable && isMouseHovering);
        }

        if (this.selected && button == 0) {
            int var5 = mouseX - this.x;
            if (this.shouldDrawBackground) {
                var5 -= 4;
            }

            String var6 = CharacterUtils.getRenderableString(this.text.substring(this.cursorPosition), this.getBackgroundOffset(), false, textRenderer);
            this.updateCursorMax(CharacterUtils.getRenderableString(var6, var5, false, textRenderer).length() + this.cursorPosition);
        }

    }

    @Override
    public void setXYWH(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public List<String> getTooltip() {
        if (!enabled) {
            return Collections.singletonList("Server synced, you cannot change this value");
        }
        if (contentsValidator != null) {
            return contentsValidator.apply(getText());
        }
        return null;
    }

    @Override
    public int[] getXYWH() {
        return new int[]{x, y, width, height};
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (doRenderUpdate) {
            onTextChanged();
            doRenderUpdate = false;
        }
        if (this.shouldDrawBackground()) {
            fill(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, isValueValid()? enabled? -6250336 : serverSyncedBorder : errorBorderColour);
            fill(this.x, this.y, this.x + this.width, this.y + this.height, -16777216);
        }

        int var1 = this.focusable ? enabled? this.selectedTextColour : serverSyncedText : this.deselectedTextColour;
        int var2 = this.cursorMax - this.cursorPosition;
        int var3 = this.cursorMin - this.cursorPosition;
        String var4 = CharacterUtils.getRenderableString(this.text.substring(this.cursorPosition), this.getBackgroundOffset(), false, textRenderer);
        boolean var5 = var2 >= 0 && var2 <= var4.length();
        boolean var6 = this.selected && this.focusedTicks / 6 % 2 == 0 && var5;
        int firstStringPos = this.shouldDrawBackground ? this.x + 4 : this.x;
        int textY = this.shouldDrawBackground ? this.y + (this.height - 8) / 2 : this.y;
        int secondStringPos = firstStringPos;
        if (var3 > var4.length()) {
            var3 = var4.length();
        }

        if (!var4.isEmpty()) {
            String firstString = var5 ? var4.substring(0, var2) : var4;
            this.textRenderer.drawWithShadow(firstString, firstStringPos, textY, var1);
            secondStringPos += textRenderer.getWidth(firstString);
            secondStringPos++;
        }

        boolean var13 = this.cursorMax < this.text.length() || this.text.length() >= this.getMaxLength();
        int selectStart = secondStringPos;
        if (!var5) {
            selectStart = var2 > 0 ? firstStringPos + this.width : firstStringPos;
        } else if (var13) {
            selectStart = --secondStringPos;
        }

        if (!var4.isEmpty() && var5 && var2 < var4.length()) {
            this.textRenderer.drawWithShadow(var4.substring(var2), secondStringPos, textY, var1);
        }

        if (var6) {
            if (var13) {
                fill(selectStart, textY - 1, selectStart + 1, textY + (this.height /2) - 2, -3092272);
            } else {
                this.textRenderer.drawWithShadow("_", selectStart, textY, var1);
            }
        }

        if (var3 != var2) {
            int var12 = firstStringPos + this.textRenderer.getWidth(var4.substring(0, var3));
            this.drawHighlightOverlay(selectStart, textY - 1, var12 - 1, textY + (this.height /2));
        }

    }

    private void drawHighlightOverlay(int x, int y, int width, int height) {
        int topLeftCorner;
        if (x < width) {
            topLeftCorner = x;
            x = width;
            width = topLeftCorner;
        }

        if (y < height) {
            topLeftCorner = y;
            y = height;
            height = topLeftCorner;
        }

        Tessellator var6 = Tessellator.INSTANCE;
        GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
        GL11.glDisable(3553);
        GL11.glEnable(3058);
        GL11.glLogicOp(5387);
        var6.startQuads();
        var6.vertex(x, height, 0.0D);
        var6.vertex(width, height, 0.0D);
        var6.vertex(width, y, 0.0D);
        var6.vertex(x, y, 0.0D);
        var6.draw();
        GL11.glDisable(3058);
        GL11.glEnable(3553);
    }

    public void setMaxLength(int i) {
        this.maxLength = i;
        if (this.text.length() > i) {
            this.text = this.text.substring(0, i);
        }

    }

    public boolean shouldDrawBackground() {
        return this.shouldDrawBackground;
    }

    @SuppressWarnings("unused")
    public void setShouldDrawBackground(boolean flag) {
        this.shouldDrawBackground = flag;
    }

    public void setSelected(boolean flag) {
        if (flag && !this.selected) {
            this.focusedTicks = 0;
        }

        this.selected = flag;
    }

    @SuppressWarnings("unused")
    public boolean isSelected() {
        return this.selected;
    }

    public int getBackgroundOffset() {
        return this.shouldDrawBackground() ? this.width - 8 : this.width;
    }

    public void updateCursorPosition(int newCursorPos) {
        int var2 = this.text.length();
        if (newCursorPos > var2) {
            newCursorPos = var2;
        }

        if (newCursorPos < 0) {
            newCursorPos = 0;
        }

        this.cursorMin = newCursorPos;
        if (this.textRenderer != null) {
            if (this.cursorPosition > var2) {
                this.cursorPosition = var2;
            }

            int backgroundOffset = this.getBackgroundOffset();
            String visibleString = CharacterUtils.getRenderableString(this.text.substring(this.cursorPosition), backgroundOffset, false, textRenderer);
            int var5 = visibleString.length() + this.cursorPosition;
            if (newCursorPos == this.cursorPosition) {
                this.cursorPosition -= CharacterUtils.getRenderableString(this.text, backgroundOffset, true, textRenderer).length();
            }

            if (newCursorPos > var5) {
                this.cursorPosition += newCursorPos - var5;
            } else if (newCursorPos <= this.cursorPosition) {
                this.cursorPosition -= this.cursorPosition - newCursorPos;
            }

            if (this.cursorPosition < 0) {
                this.cursorPosition = 0;
            }

            if (this.cursorPosition > var2) {
                this.cursorPosition = var2;
            }
        }

    }

    public void setValidator(Function<String, List<String>> contentsValidator) {
        this.contentsValidator = contentsValidator;
    }
}
