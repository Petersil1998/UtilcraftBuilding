package net.petersil98.utilcraft_building.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderProperties;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.block_entities.BlueprintBlockEntity;
import net.petersil98.utilcraft_building.blocks.BlueprintBlock;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

public class BlueprintBlockTileEntityRenderer implements BlockEntityRenderer<BlueprintBlockEntity> {

    public static final ResourceLocation CUBE_MODEL = new ResourceLocation(UtilcraftBuilding.MOD_ID, "block/blueprint_block_cube");

    private final BlockModelShaper shapes;
    private final BlockColors colors;
    private final Minecraft minecraftInstance;

    public BlueprintBlockTileEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.minecraftInstance = Minecraft.getInstance();
        this.shapes = this.minecraftInstance.getModelManager().getBlockModelShaper();
        this.colors = this.minecraftInstance.getBlockColors();
    }

    public void render(@Nonnull BlueprintBlockEntity tileEntity, float partialTicks, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Level world = tileEntity.getLevel();
        BlockState blockstate = world != null ? tileEntity.getBlockState() : UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.get().defaultBlockState();
        Block block = blockstate.getBlock();
        if (block instanceof BlueprintBlock) {
            matrixStack.pushPose();
            renderRotatingCube(tileEntity, matrixStack, buffer, combinedLight, combinedOverlay);
            matrixStack.popPose();
            renderLayoutBlocks(tileEntity, matrixStack, buffer, combinedLight, combinedOverlay);
        }
    }

    private void renderRotatingCube(@Nonnull BlueprintBlockEntity blockEntity, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
        BakedModel model = this.minecraftInstance.getModelManager().getModel(CUBE_MODEL);

        long time = System.currentTimeMillis();

        int speed = 5;
        long angle = (time / speed) % 360;
        Quaternion rotation = new Vector3f(0f,1f,0).rotationDegrees(angle);

        matrixStack.translate(.5, .5, .5);
        matrixStack.mulPose(rotation);
        matrixStack.translate(-.5, -.5, -.5);

        int color = this.colors.getColor(blockEntity.getBlockState(), null, null, 0);
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        this.minecraftInstance.getBlockRenderer().getModelRenderer().renderModel(matrixStack.last(), builder, blockEntity.getBlockState(), model, red, green, blue, combinedLight, combinedOverlay, EmptyModelData.INSTANCE);
    }

    private void renderLayoutBlocks(@Nonnull BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        Direction direction = tileEntity.getBlockState().getValue(BlueprintBlock.FACING);
        switch (direction) {
            case SOUTH -> renderLayoutBlocksSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
            case WEST -> renderLayoutBlocksWest(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
            case EAST -> renderLayoutBlocksEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
            default -> renderLayoutBlocksNorth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
        }
    }

    private void renderLayoutBlocksNorth(BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksNorthSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, true);
    }

    private void renderLayoutBlocksSouth(BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksNorthSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, false);
    }

    private void renderLayoutBlocksWest(BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksWestEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, true);
    }

    private void renderLayoutBlocksEast(BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksWestEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, false);
    }

    private void renderLayoutBlocksNorthSouth(@Nonnull BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay, boolean isNorth) {
        int sign = isNorth ? 1 : -1;
        matrixStack.pushPose();
        tileEntity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            List<List<List<BlockState>>> pattern = iBluePrint.getPattern();
            for(int i = 0; i < pattern.size(); i++) {
                BlockPos currentY = tileEntity.getBlockPos().above(i);
                matrixStack.translate( 0, 0, sign*(-pattern.get(i).size()/2));
                for(int j = 0; j < pattern.get(i).size(); j++) {
                    int posZ = sign*(pattern.get(i).size()/2-j);
                    BlockPos currentXY = currentY.north(posZ);
                    matrixStack.translate(sign*(-pattern.get(i).get(j).size()/2), 0, 0);
                    for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                        int posX = sign*(pattern.get(i).get(j).size()/2-k);
                        BlockState state = pattern.get(i).get(j).get(k);
                        if(!tileEntity.getLevel().getBlockState(currentXY.west(posX)).getMaterial().isSolid() && !(state.getBlock() instanceof AirBlock)) {
                            renderBlockWithTransparency(state, matrixStack, bufferType, combinedLight, combinedOverlay);
                        }
                        matrixStack.translate(sign,0,0);
                    }
                    matrixStack.translate(sign*(-pattern.get(i).get(j).size()/2), 0, sign);
                }
                matrixStack.translate(0, 1, sign*(-pattern.get(i).size() /2));
            }
            matrixStack.translate(0,-pattern.size(), 0);
        });
        matrixStack.popPose();
    }

    private void renderLayoutBlocksWestEast(@Nonnull BlueprintBlockEntity tileEntity, @Nonnull PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay, boolean isWest) {
        int sign = isWest ? 1 : -1;
        matrixStack.pushPose();
        tileEntity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            List<List<List<BlockState>>> pattern = iBluePrint.getPattern();
            for(int i = 0; i < pattern.size(); i++) {
                BlockPos currentY = tileEntity.getBlockPos().above(i);
                matrixStack.translate( sign*(-pattern.get(i).size()/2), 0, 0);
                for(int j = 0; j < pattern.get(i).size(); j++) {
                    int posX = sign*(pattern.get(i).size()/2-j);
                    BlockPos currentXY = currentY.west(posX);
                    matrixStack.translate(0, 0, sign*(pattern.get(i).get(j).size()/2));
                    for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                        int posZ = sign*(pattern.get(i).get(j).size()/2-k);
                        BlockState state = pattern.get(i).get(j).get(k);
                        if(!tileEntity.getLevel().getBlockState(currentXY.south(posZ)).getMaterial().isSolid() && !(state.getBlock() instanceof AirBlock)) {
                            renderBlockWithTransparency(state, matrixStack, bufferType, combinedLight, combinedOverlay);
                        }
                        matrixStack.translate(0,0,-sign);
                    }
                    matrixStack.translate(sign, 0, sign*(pattern.get(i).get(j).size()/2));
                }
                matrixStack.translate( sign*(-pattern.get(i).size() /2), 1, 0);
            }
            matrixStack.translate(0,-pattern.size(), 0);
        });
        matrixStack.popPose();
    }

    private void renderBlockWithTransparency(@Nonnull BlockState blockState, PoseStack matrixStack, MultiBufferSource bufferType, int combinedLight, int combinedOverlay) {
        RenderShape blockrendertype = blockState.getRenderShape();
        if (blockrendertype != RenderShape.INVISIBLE) {
            switch (blockrendertype) {
                case MODEL -> {
                    BakedModel bakedModel = this.shapes.getBlockModel(blockState);
                    int color = this.colors.getColor(blockState, null, null, 0);
                    float red = (float) (color >> 16 & 255) / 255.0F;
                    float green = (float) (color >> 8 & 255) / 255.0F;
                    float blue = (float) (color & 255) / 255.0F;
                    renderModel(matrixStack.last(), bufferType.getBuffer(RenderType.translucent()), blockState, bakedModel, red, green, blue, combinedLight, combinedOverlay);
                }
                case ENTITYBLOCK_ANIMATED -> {
                    ItemStack stack = new ItemStack(blockState.getBlock());
                    RenderProperties.get(stack).getItemStackRenderer().renderByItem(stack, ItemTransforms.TransformType.NONE, matrixStack, bufferType, combinedLight, combinedOverlay);
                }
            }
        }
    }

    private void renderModel(PoseStack.Pose matrixEntry, VertexConsumer buffer, @Nullable BlockState state, BakedModel model, float red, float green, float blue, int combinedLight, int combinedOverlay) {
        Random random = new Random();
        long seed = 42L;

        for(Direction direction : Direction.values()) {
            random.setSeed(seed);
            renderModelBrightnessColorQuads(matrixEntry, buffer, red, green, blue, model.getQuads(state, direction, random, EmptyModelData.INSTANCE), combinedLight, combinedOverlay);
        }
        random.setSeed(seed);
        renderModelBrightnessColorQuads(matrixEntry, buffer, red, green, blue, model.getQuads(state, null, random, EmptyModelData.INSTANCE), combinedLight, combinedOverlay);
    }

    private void renderModelBrightnessColorQuads(PoseStack.Pose matrixEntry, VertexConsumer buffer, float r, float g, float b, @Nonnull List<BakedQuad> listQuads, int combinedLight, int combinedOverlay) {
        for(BakedQuad bakedquad : listQuads) {
            float red;
            float green;
            float blue;
            if (bakedquad.isTinted()) {
                red = Mth.clamp(r, 0.0F, 1.0F);
                green = Mth.clamp(g, 0.0F, 1.0F);
                blue = Mth.clamp(b, 0.0F, 1.0F);
            } else {
                red = 1.0F;
                green = 1.0F;
                blue = 1.0F;
            }

            int[] vertexData = bakedquad.getVertices();
            Vec3i vector3i = bakedquad.getDirection().getNormal();
            Vector3f vector3f = new Vector3f((float)vector3i.getX(), (float)vector3i.getY(), (float)vector3i.getZ());
            Matrix4f matrix4f = matrixEntry.pose();
            vector3f.transform(matrixEntry.normal());
            int size = 8;
            int j = vertexData.length / size;

            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
                IntBuffer intbuffer = bytebuffer.asIntBuffer();
                int[] cLights = new int[]{combinedLight, combinedLight, combinedLight, combinedLight};
                for(int k = 0; k < j; ++k) {
                    intbuffer.clear();
                    intbuffer.put(vertexData, k * size, size);
                    float f = bytebuffer.getFloat(0);
                    float f1 = bytebuffer.getFloat(size/2);
                    float f2 = bytebuffer.getFloat(size);

                    int l = this.applyBakedLighting(cLights[k], bytebuffer);
                    float f9 = bytebuffer.getFloat(16);
                    float f10 = bytebuffer.getFloat(20);
                    Vector4f vector4f = new Vector4f(f, f1, f2, 1.0F);
                    vector4f.transform(matrix4f);
                    this.applyBakedNormals(vector3f, bytebuffer, matrixEntry.normal());
                    buffer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), red, green, blue, 0.85F, f9, f10, combinedOverlay, l, vector3f.x(), vector3f.y(), vector3f.z());
                }
            }
        }
    }

    private int applyBakedLighting(int lightmapCoord, @Nonnull ByteBuffer data) {
        int bl = lightmapCoord&0xFFFF;
        int sl = (lightmapCoord>>16)&0xFFFF;
        int offset = LightUtil.getLightOffset(0) * 4;
        int blBaked = Short.toUnsignedInt(data.getShort(offset));
        int slBaked = Short.toUnsignedInt(data.getShort(offset + 2));
        bl = Math.max(bl, blBaked);
        sl = Math.max(sl, slBaked);
        return bl | (sl<<16);
    }

    private void applyBakedNormals(Vector3f generated, @Nonnull ByteBuffer data, Matrix3f normalTransform) {
        byte nx = data.get(28);
        byte ny = data.get(29);
        byte nz = data.get(30);
        if (nx != 0 || ny != 0 || nz != 0) {
            generated.set(nx / 127f, ny / 127f, nz / 127f);
            generated.transform(normalTransform);
        }
    }
}
