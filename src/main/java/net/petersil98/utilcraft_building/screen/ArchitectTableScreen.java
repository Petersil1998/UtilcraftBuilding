package net.petersil98.utilcraft_building.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.network.SyncButtonPressed;

import javax.annotation.Nonnull;

//TODO: Fix resizing
public class ArchitectTableScreen extends ContainerScreen<ArchitectTableContainer> {

    public static final int PREVIOUS_BUTTON_ID = 0;
    public static final int NEXT_BUTTON_ID = 1;
    public static final int ADD_LAYER_BUTTON_ID = 2;
    public static final int REMOVE_LAYER_BUTTON_ID = 3;

    private static final ResourceLocation ARCHITECT_TABLE_GUI_TEXTURES = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/architect_table_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/button_arrow.png");
    private TranslationTextComponent layerText = new TranslationTextComponent(String.format("architect_table.%s.layer", UtilcraftBuilding.MOD_ID),this.menu.getCurrentLayer()+1, this.menu.getCurrentMaxLayers()+1);
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton addLayerButton;
    private ImageButton removeLayerButton;
    private boolean widthTooNarrow;

    public ArchitectTableScreen(ArchitectTableContainer screenContainer, PlayerInventory inv, ITextComponent title) {
        super(screenContainer, inv, title);
    }

    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.titleLabelY -= 65;
        this.inventoryLabelY += 65;
        this.imageHeight += 130;
        this.imageWidth += 60;
        this.previousButton = new ImageButton(this.leftPos + 5, this.height / 2 - 49, 20, 18, 0, 0, 0, BUTTON_TEXTURE, button -> previousLayer());
        this.nextButton = new ImageButton(this.leftPos + this.imageWidth - 30, this.height / 2 - 49, 20, 18, 0, 18, 0, BUTTON_TEXTURE, button -> nextLayer());
        this.addLayerButton = new ImageButton(this.leftPos + this.imageWidth - 45, this.height/2 + 50, 20, 18, 0, 0, 0, BUTTON_TEXTURE, button -> addLayer());
        this.removeLayerButton = new ImageButton(this.leftPos + this.imageWidth - 45, this.height/2 + 80, 20, 18, 0, 18, 0, BUTTON_TEXTURE, button -> removeLayer());
    }

    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        if (this.widthTooNarrow) {
            this.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        } else {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void tick() {
        super.tick();
        this.updateTitle();
        this.updateButtons();
    }

    protected void renderBg(@Nonnull MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(ARCHITECT_TABLE_GUI_TEXTURES);
        int i = this.leftPos;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
        AbstractGui.blit(matrixStack, i, j, this.getBlitOffset(), 0,0, this.imageWidth, this.imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(@Nonnull MatrixStack matrixStack, int x, int y) {
        super.renderLabels(matrixStack, x, y);

        int stringWidth = this.font.width(this.layerText.getString());
        this.font.draw(matrixStack, this.layerText, this.imageWidth - stringWidth - 10, this.titleLabelY, 4210752);

        this.addButton(this.previousButton);
        this.addButton(this.nextButton);
        this.addButton(this.addLayerButton);
        this.addButton(this.removeLayerButton);
    }

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return !this.widthTooNarrow && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.widthTooNarrow || super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < (double)guiLeft || mouseY < (double)guiTop || mouseX >= (double)(guiLeft + this.imageWidth) || mouseY >= (double)(guiTop + this.imageHeight);
    }

    private void nextLayer() {
        this.menu.nextLayer();
        PacketHandler.sendToServer(new SyncButtonPressed(NEXT_BUTTON_ID));
    }

    private void previousLayer() {
        this.menu.previousLayer();
        PacketHandler.sendToServer(new SyncButtonPressed(PREVIOUS_BUTTON_ID));
    }

    private void addLayer() {
        this.menu.addLayer();
        PacketHandler.sendToServer(new SyncButtonPressed(ADD_LAYER_BUTTON_ID));
    }

    private void removeLayer() {
        this.menu.removeCurrentLayer();
        PacketHandler.sendToServer(new SyncButtonPressed(REMOVE_LAYER_BUTTON_ID));
    }

    private void updateButtons() {
        boolean check = this.menu.hasNextLayer();
        this.nextButton.active = check;
        this.nextButton.visible = check;
        check = this.menu.hasPreviousLayer();
        this.previousButton.active = check;
        this.previousButton.visible = check;
        check = this.menu.containsBlueprint() && this.menu.getCurrentMaxLayers() >= 1;
        this.removeLayerButton.active = check;
        this.removeLayerButton.visible = check;
        check = this.menu.containsBlueprint() && this.menu.getCurrentMaxLayers() < ArchitectTableContainer.MAX_LAYERS;
        this.addLayerButton.active = check;
        this.addLayerButton.visible = check;
    }

    private void updateTitle() {
        this.layerText = new TranslationTextComponent(this.layerText.getKey(), this.menu.getCurrentLayer()+1, this.menu.getCurrentMaxLayers()+1);
    }
}
