package net.petersil98.utilcraft_building.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;
import net.petersil98.utilcraft_building.network.CreateStructurePacket;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

public class BlueprintBlockScreen extends AbstractContainerScreen<BlueprintBlockContainer> {
    /** The ResourceLocation containing the chest GUI texture. */
    private static final ResourceLocation INVENTORY_GUI_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/blueprint_block_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/button_arrow.png");
    private ImageButton constructButton;
    private boolean shouldUpdateButtons = true;

    public BlueprintBlockScreen(BlueprintBlockContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    protected void init() {
        super.init();
        this.constructButton = new ImageButton(this.leftPos + 45, this.height/2 + 80, 20, 18, 0, 0, 0, BUTTON_TEXTURE, button -> constructStructure());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if(this.shouldUpdateButtons) {
            this.updateButtons();
        }
    }

    public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    protected void renderBg(@Nonnull PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, INVENTORY_GUI_TEXTURE);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack matrixStack, int x, int y) {
        super.renderLabels(matrixStack, x, y);

        this.addRenderableWidget(this.constructButton);
    }

    private void constructStructure() {
        this.shouldUpdateButtons = false;
        PacketHandler.sendToServer(new CreateStructurePacket());
    }

    private void updateButtons() {
        Map<Block, Integer> required = BlueprintUtils.fromBlockStateToBlock(BlueprintUtils.listBlockStatesFromCapability(this.menu.getTileEntity()));
        Map<Item, Integer> supplied = BlueprintUtils.listBlockItemsFromInventory(this.menu.getContainerInventory());
        Map<Block, Integer> blocksSupplied = supplied.entrySet().stream().filter(e -> e.getKey() instanceof BlockItem).collect(Collectors.toMap((e -> ((BlockItem) e.getKey()).getBlock()), Map.Entry::getValue));
        boolean enabled = true;
        if(blocksSupplied.size() >= required.size()) {
            for(Map.Entry<Block, Integer> requiredEntry: required.entrySet()) {
                if(!blocksSupplied.containsKey(requiredEntry.getKey()) || requiredEntry.getValue() > blocksSupplied.get(requiredEntry.getKey())) {
                    enabled = false;
                    break;
                }
            }
        } else {
            enabled = false;
        }
        this.constructButton.active = enabled;
        this.constructButton.visible = enabled;
    }
}
