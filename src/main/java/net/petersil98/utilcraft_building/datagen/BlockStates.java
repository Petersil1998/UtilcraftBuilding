package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.petersil98.utilcraft_building.utils.BlockItemUtils;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;

public class BlockStates extends BlockStateProvider {

    public BlockStates(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, UtilcraftBuilding.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
    }

    private void registerSlab(SlabBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        slabBlock(block, location, location);
    }

    private void registerStairs(StairBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        stairsBlock(block, location);
    }

    private void registerWall(WallBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        wallBlock(block, location);
        models().singleTexture(BlockItemUtils.name(block)+"_inventory", mcLoc(ModelProvider.BLOCK_FOLDER+"/wall_inventory"), "wall", location);

    }

    private void registerButton(ButtonBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        ModelFile button = models().singleTexture(BlockItemUtils.name(block), mcLoc(ModelProvider.BLOCK_FOLDER+"/button"), location);
        ModelFile pressed = models().singleTexture(BlockItemUtils.name(block)+"_pressed", mcLoc(ModelProvider.BLOCK_FOLDER+"/button_pressed"), location);
        models().singleTexture(BlockItemUtils.name(block)+"_inventory", mcLoc(ModelProvider.BLOCK_FOLDER+"/button_inventory"), location);
        getVariantBuilder(block).forAllStates(blockState -> {
            Direction facing = blockState.getValue(ButtonBlock.FACING);
            AttachFace face = blockState.getValue(ButtonBlock.FACE);
            boolean powered = blockState.getValue(ButtonBlock.POWERED);
            int yRot = 0;
            int xRot = 0;
            boolean uvlock = false;
            switch (facing) {
                case EAST: {
                    yRot = 270;
                    break;
                }
                case WEST: {
                    yRot = 90;
                    break;
                }
                case NORTH: {
                    yRot = 180;
                    break;
                }
            }
            switch (face) {
                case WALL: {
                    xRot = 90;
                    uvlock = true;
                    break;
                }
                case CEILING: {
                    xRot = 180;
                    break;
                }
            }
            return ConfiguredModel.builder()
                    .modelFile(powered ? pressed : button)
                    .rotationX(xRot)
                    .rotationY(yRot)
                    .uvLock(uvlock)
                    .build();
        });
    }

    private void registerDoor(DoorBlock block, Block texture) {
        ResourceLocation bottom = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture)+"_bottom");
        ResourceLocation top = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture)+"_top");
        doorBlock(block, bottom, top);
    }

    private void registerFence(FenceBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        fenceBlock(block, location);
        models().singleTexture(BlockItemUtils.name(block)+"_inventory", mcLoc(ModelProvider.BLOCK_FOLDER+"/fence_inventory"), location);
    }

    private void registerFenceGate(FenceGateBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        fenceGateBlock(block, location);
    }

    private void registerLog(RotatedPillarBlock block) {
        logBlock(block);
    }

    private void registerPressurePlate(PressurePlateBlock block, Block texture) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(texture), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(texture));
        ModelFile pressurePlateUp = models().singleTexture(BlockItemUtils.name(block), mcLoc(ModelProvider.BLOCK_FOLDER+"/pressure_plate_up"), location);
        ModelFile pressurePlateDown = models().singleTexture(BlockItemUtils.name(block)+"_down", mcLoc(ModelProvider.BLOCK_FOLDER+"/pressure_plate_down"), location);
        getVariantBuilder(block).forAllStates(blockState -> ConfiguredModel.builder()
                .modelFile(blockState.getValue(PressurePlateBlock.POWERED) ? pressurePlateDown : pressurePlateUp)
                .build());
    }

    private void registerSapling(SaplingBlock block) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(block), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(block));
        simpleBlock(block, ConfiguredModel.builder().modelFile(models().cross(BlockItemUtils.name(block), location)).build());
    }

    private void registerSign(SignBlock block, Block particles) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(particles), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(particles));
        ModelFile particle = models().singleTexture(BlockItemUtils.name(block), mcLoc(ModelProvider.BLOCK_FOLDER+"/block"), "particle", location);
        simpleBlock(block, ConfiguredModel.builder().modelFile(particle).build());
    }

    private void registerTrapdoor(TrapDoorBlock block) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(block), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(block));
        trapdoorBlock(block, location, false);
    }

    private void registerWallSign(WallSignBlock block, SignBlock signBlock, Block particles) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(particles), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(particles));
        ModelFile particle = models().singleTexture(BlockItemUtils.name(signBlock), mcLoc(ModelProvider.BLOCK_FOLDER+"/block"), "particle", location);
        simpleBlock(block, ConfiguredModel.builder().modelFile(particle).build());
    }

    private void registerChest(Block block, Block particles) {
        ResourceLocation location = new ResourceLocation(BlockItemUtils.namespace(particles), ModelProvider.BLOCK_FOLDER +"/"+BlockItemUtils.name(particles));
        ModelFile particle = models().singleTexture(BlockItemUtils.name(block), mcLoc(ModelProvider.BLOCK_FOLDER+"/block"), "particle", location);
        simpleBlock(block, ConfiguredModel.builder().modelFile(particle).build());
    }
}
