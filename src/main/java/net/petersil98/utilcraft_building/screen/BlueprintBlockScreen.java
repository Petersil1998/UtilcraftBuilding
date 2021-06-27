package net.petersil98.utilcraft_building.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;
import net.petersil98.utilcraft_building.network.CreateStructurePacket;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

public class BlueprintBlockScreen extends ContainerScreen<BlueprintBlockContainer> {
    /** The ResourceLocation containing the chest GUI texture. */
    private static final ResourceLocation INVENTORY_GUI_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/blueprint_block_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(UtilcraftBuilding.MOD_ID, "textures/gui/button_arrow.png");
    private ImageButton constructButton;
    private boolean shouldUpdateButtons = true;

    public BlueprintBlockScreen(BlueprintBlockContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        this.playerInventoryTitleY = this.ySize - 92;
    }

    @Override
    protected void init() {
        super.init();
        this.constructButton = new ImageButton(this.guiLeft + 45, this.height/2 + 80, 20, 18, 0, 0, 0, BUTTON_TEXTURE, button -> constructStructure());
    }

    @Override
    public void tick() {
        super.tick();
        if(this.shouldUpdateButtons) {
            this.updateButtons();
        }
    }

    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    protected void drawGuiContainerBackgroundLayer(@Nonnull MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(INVENTORY_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
        this.blit(matrixStack, i, j ,0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(@Nonnull MatrixStack matrixStack, int x, int y) {
        super.drawGuiContainerForegroundLayer(matrixStack, x, y);

        this.addButton(this.constructButton);
    }

    private void constructStructure() {
        this.shouldUpdateButtons = false;
        PacketHandler.sendToServer(new CreateStructurePacket());
    }

    private void updateButtons() {
        Map<Block, Integer> required = BlueprintUtils.fromBlockStateToBlock(BlueprintUtils.listBlockStatesFromCapability(this.container.getTileEntity()));
        Map<Item, Integer> supplied = BlueprintUtils.listBlockItemsFromInventory(this.container.getContainerInventory());
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
