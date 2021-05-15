package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraft.block.BlockState;

import java.util.List;

public interface IBluePrint {

    void setPattern(List<List<List<BlockState>>> pattern);

    List<List<List<BlockState>>> getPattern();
}
