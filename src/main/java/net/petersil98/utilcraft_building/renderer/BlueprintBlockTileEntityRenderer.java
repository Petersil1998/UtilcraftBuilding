package net.petersil98.utilcraft_building.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.BlueprintBlock;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

public class BlueprintBlockTileEntityRenderer extends TileEntityRenderer<BlueprintBlockTileEntity> {

    public static final ResourceLocation CUBE_MODEL = new ResourceLocation(UtilcraftBuilding.MOD_ID, "block/blueprint_block_cube");

    private final BlockModelShapes shapes;
    private final BlockColors colors;
    private final Minecraft instance;

    public BlueprintBlockTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcher) {
        super(rendererDispatcher);
        this.instance = Minecraft.getInstance();
        this.shapes = this.instance.getModelManager().getBlockModelShapes();
        this.colors = this.instance.getBlockColors();
    }

    public void render(@Nonnull BlueprintBlockTileEntity tileEntity, float partialTicks, @Nonnull MatrixStack matrixStack, @Nonnull IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        World world = tileEntity.getWorld();
        BlockState blockstate = world != null ? tileEntity.getBlockState() : UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.getDefaultState();
        Block block = blockstate.getBlock();
        if (block instanceof BlueprintBlock) {
            matrixStack.push();
            renderRotatingCube(tileEntity, matrixStack, buffer);
            matrixStack.pop();
            renderLayoutBlocks(tileEntity, matrixStack, buffer, combinedLight, combinedOverlay);
        }
    }

    private void renderRotatingCube(@Nonnull BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, @Nonnull IRenderTypeBuffer buffer) {
        Random rnd = new Random(tileEntity.getPos().getX() * 337L + tileEntity.getPos().getY() * 37L + tileEntity.getPos().getZ() * 13L);
        IVertexBuilder builder = buffer.getBuffer(RenderType.getTranslucent());
        IBakedModel model = this.instance.getModelManager().getModel(CUBE_MODEL);

        long time = System.currentTimeMillis();

        int speed = 5;
        long angle = (time / speed) % 360;
        Quaternion rotation = new Vector3f(0f,1f,0).rotationDegrees(angle);

        matrixStack.translate(.5, .5, .5);
        matrixStack.rotate(rotation);
        matrixStack.translate(-.5, -.5, -.5);

        this.instance.getBlockRendererDispatcher().getBlockModelRenderer().renderModel(this.instance.world, model, tileEntity.getBlockState(), tileEntity.getPos(), matrixStack, builder, true, rnd, tileEntity.getBlockState().getPositionRandom(tileEntity.getPos()), OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
    }

    private void renderLayoutBlocks(@Nonnull BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        Direction direction = tileEntity.getBlockState().get(BlueprintBlock.FACING);
        switch (direction) {
            case SOUTH: {
                renderLayoutBlocksSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
                break;
            }
            case WEST: {
                renderLayoutBlocksWest(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
                break;
            }
            case EAST: {
                renderLayoutBlocksEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
                break;
            }
            default: {
                renderLayoutBlocksNorth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay);
                break;
            }
        }
    }

    private void renderLayoutBlocksNorth(BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksNorthSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, true);
    }

    private void renderLayoutBlocksSouth(BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksNorthSouth(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, false);
    }

    private void renderLayoutBlocksWest(BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksWestEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, true);
    }

    private void renderLayoutBlocksEast(BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        renderLayoutBlocksWestEast(tileEntity, matrixStack, bufferType, combinedLight, combinedOverlay, false);
    }

    private void renderLayoutBlocksNorthSouth(@Nonnull BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay, boolean isNorth) {
        int sign = isNorth ? 1 : -1;
        matrixStack.push();
        tileEntity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            List<List<List<BlockState>>> pattern = iBluePrint.getPattern();
            for(int i = 0; i < pattern.size(); i++) {
                BlockPos currentY = tileEntity.getPos().up(i);
                matrixStack.translate( 0, 0, sign*(-pattern.get(i).size()/2));
                for(int j = 0; j < pattern.get(i).size(); j++) {
                    int posZ = sign*(pattern.get(i).size()/2-j);
                    BlockPos currentXY = currentY.north(posZ);
                    matrixStack.translate(sign*(-pattern.get(i).get(j).size()/2), 0, 0);
                    for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                        int posX = sign*(pattern.get(i).get(j).size()/2-k);
                        BlockState state = pattern.get(i).get(j).get(k);
                        if(!tileEntity.getWorld().getBlockState(currentXY.west(posX)).getMaterial().isSolid() && !(state.getBlock() instanceof AirBlock)) {
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
        matrixStack.pop();
    }

    private void renderLayoutBlocksWestEast(@Nonnull BlueprintBlockTileEntity tileEntity, @Nonnull MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay, boolean isWest) {
        int sign = isWest ? 1 : -1;
        matrixStack.push();
        tileEntity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            List<List<List<BlockState>>> pattern = iBluePrint.getPattern();
            for(int i = 0; i < pattern.size(); i++) {
                BlockPos currentY = tileEntity.getPos().up(i);
                matrixStack.translate( sign*(-pattern.get(i).size()/2), 0, 0);
                for(int j = 0; j < pattern.get(i).size(); j++) {
                    int posX = sign*(pattern.get(i).size()/2-j);
                    BlockPos currentXY = currentY.west(posX);
                    matrixStack.translate(0, 0, sign*(pattern.get(i).get(j).size()/2));
                    for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                        int posZ = sign*(pattern.get(i).get(j).size()/2-k);
                        BlockState state = pattern.get(i).get(j).get(k);
                        if(!tileEntity.getWorld().getBlockState(currentXY.north(posZ)).getMaterial().isSolid() && !(state.getBlock() instanceof AirBlock)) {
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
        matrixStack.pop();
    }

    private void renderBlockWithTransparency(@Nonnull BlockState blockState, MatrixStack matrixStack, IRenderTypeBuffer bufferType, int combinedLight, int combinedOverlay) {
        BlockRenderType blockrendertype = blockState.getRenderType();
        if (blockrendertype != BlockRenderType.INVISIBLE) {
            switch(blockrendertype) {
                case MODEL:
                    IBakedModel bakedmodel = this.shapes.getModel(blockState);
                    int color = this.colors.getColor(blockState, null, null, 0);
                    float red = (float)(color >> 16 & 255) / 255.0F;
                    float green = (float)(color >> 8 & 255) / 255.0F;
                    float blue = (float)(color & 255) / 255.0F;
                    renderModel(matrixStack.getLast(), bufferType.getBuffer(RenderType.getTranslucent()), blockState, bakedmodel, red, green, blue, combinedLight, combinedOverlay);
                    break;
                case ENTITYBLOCK_ANIMATED:
                    ItemStack stack = new ItemStack(blockState.getBlock());
                    stack.getItem().getItemStackTileEntityRenderer().func_239207_a_(stack, ItemCameraTransforms.TransformType.NONE, matrixStack, bufferType, combinedLight, combinedOverlay);
            }
        }
    }

    private void renderModel(MatrixStack.Entry matrixEntry, IVertexBuilder buffer, @Nullable BlockState state, IBakedModel model, float red, float green, float blue, int combinedLight, int combinedOverlay) {
        Random random = new Random();
        long seed = 42L;

        for(Direction direction : Direction.values()) {
            random.setSeed(seed);
            renderModelBrightnessColorQuads(matrixEntry, buffer, red, green, blue, model.getQuads(state, direction, random, EmptyModelData.INSTANCE), combinedLight, combinedOverlay);
        }
        random.setSeed(seed);
        renderModelBrightnessColorQuads(matrixEntry, buffer, red, green, blue, model.getQuads(state, null, random, EmptyModelData.INSTANCE), combinedLight, combinedOverlay);
    }

    private void renderModelBrightnessColorQuads(MatrixStack.Entry matrixEntry, IVertexBuilder buffer, float r, float g, float b, @Nonnull List<BakedQuad> listQuads, int combinedLight, int combinedOverlay) {
        for(BakedQuad bakedquad : listQuads) {
            float red;
            float green;
            float blue;
            if (bakedquad.hasTintIndex()) {
                red = MathHelper.clamp(r, 0.0F, 1.0F);
                green = MathHelper.clamp(g, 0.0F, 1.0F);
                blue = MathHelper.clamp(b, 0.0F, 1.0F);
            } else {
                red = 1.0F;
                green = 1.0F;
                blue = 1.0F;
            }

            int[] vertexData = bakedquad.getVertexData();
            Vector3i vector3i = bakedquad.getFace().getDirectionVec();
            Vector3f vector3f = new Vector3f((float)vector3i.getX(), (float)vector3i.getY(), (float)vector3i.getZ());
            Matrix4f matrix4f = matrixEntry.getMatrix();
            vector3f.transform(matrixEntry.getNormal());
            int size = 8;
            int j = vertexData.length / size;

            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormats.BLOCK.getSize());
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
                    this.applyBakedNormals(vector3f, bytebuffer, matrixEntry.getNormal());
                    buffer.addVertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), red, green, blue, 0.85F, f9, f10, combinedOverlay, l, vector3f.getX(), vector3f.getY(), vector3f.getZ());
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
