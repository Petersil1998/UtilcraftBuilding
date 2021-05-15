package net.petersil98.utilcraft_building.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;

import javax.annotation.Nonnull;

//TODO: Fix resizing
public class ArchitectTableScreen extends ContainerScreen<ArchitectTableContainer> {

    private static final ResourceLocation ARCHITECT_TABLE_GUI_TEXTURES = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/architect_table_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/button_arrow.png");
    private final TextComponent layerText = new TranslationTextComponent(String.format("architect_table.%s.layer", UtilcraftBuilding.MOD_ID),this.container.getLayer()+1, this.container.getMaxLayers());
    private ImageButton previousButton;
    private ImageButton nextButton;
    private boolean widthTooNarrow;

    public ArchitectTableScreen(ArchitectTableContainer screenContainer, PlayerInventory inv, ITextComponent title) {
        super(screenContainer, inv, title);
    }

    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.titleY -= 65;
        this.playerInventoryTitleY += 65;
        this.ySize += 130;
        this.xSize += 60;
        this.previousButton = new ImageButton(this.guiLeft + 5, this.height / 2 - 49, 20, 18, 0, 0, 0, BUTTON_TEXTURE, button -> previousLayer());
        this.nextButton = new ImageButton(this.guiLeft + this.xSize - 30, this.height / 2 - 49, 20, 18, 0, 18, 0, BUTTON_TEXTURE, button -> nextLayer());
        this.updateButtons();
    }

    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        if (this.widthTooNarrow) {
            this.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
        } else {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    protected void drawGuiContainerBackgroundLayer(@Nonnull MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(ARCHITECT_TABLE_GUI_TEXTURES);
        int i = this.guiLeft;
        int j = (this.height - this.ySize) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
        AbstractGui.blit(matrixStack, i, j, this.getBlitOffset(), 0,0, this.xSize, this.ySize, 512, 512);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(@Nonnull MatrixStack matrixStack, int x, int y) {
        super.drawGuiContainerForegroundLayer(matrixStack, x, y);

        int stringWidth = this.font.getStringWidth(layerText.getString());
        this.font.func_243248_b(matrixStack, layerText, this.xSize - stringWidth - 10, this.titleY, 4210752);

        this.addButton(previousButton);
        this.addButton(nextButton);
    }

    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        return !this.widthTooNarrow && super.isPointInRegion(x, y, width, height, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.widthTooNarrow || super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < (double)guiLeft || mouseY < (double)guiTop || mouseX >= (double)(guiLeft + this.xSize) || mouseY >= (double)(guiTop + this.ySize);
    }

    private void nextLayer() {
        this.container.nextLayer();
        this.updateButtons();
    }

    private void previousLayer() {
        this.container.previousLayer();
        this.updateButtons();
    }

    private void updateButtons() {
        boolean check = this.container.hasNextLayer();
        this.nextButton.active = check;
        this.nextButton.visible = check;
        check = this.container.hasPreviousLayer();
        this.previousButton.active = check;
        this.previousButton.visible = check;
    }
}
