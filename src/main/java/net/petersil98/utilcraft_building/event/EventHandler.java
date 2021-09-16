package net.petersil98.utilcraft_building.event;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.BasicTrade;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.UtilcraftBuildingItems;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = UtilcraftBuilding.MOD_ID)
public class EventHandler {

    @SubscribeEvent
    public static void villagerTrades(@Nonnull VillagerTradesEvent event){
        if(event.getType().equals(VillagerProfession.LIBRARIAN)) {
            ItemStack stack = new ItemStack(UtilcraftBuildingItems.BLUEPRINT.get());
            stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
                List<List<List<BlockState>>> pattern = new ArrayList<>();
                pattern.add(new ArrayList<>());
                for(int i = 0; i < ArchitectTableContainer.SIZE; i++) {
                    pattern.get(0).add(new ArrayList<>());
                    for(int j = 0; j < ArchitectTableContainer.SIZE; j++) {
                        pattern.get(0).get(i).add(Blocks.AIR.defaultBlockState());
                    }
                }
                pattern.get(0).get(0).set(0,Blocks.BLACK_WOOL.defaultBlockState());
                iBluePrint.setPattern(pattern);
            });
            event.getTrades().get(1).add(new BasicTrade(10, stack, 1, 10, 0.125f));
        }
    }
}
