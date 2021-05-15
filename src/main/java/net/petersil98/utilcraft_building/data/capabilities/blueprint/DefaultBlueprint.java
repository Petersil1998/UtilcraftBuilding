package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraft.block.BlockState;

import java.util.List;

public class DefaultBlueprint implements IBluePrint {

    private List<List<List<BlockState>>> pattern;

    @Override
    public void setPattern(List<List<List<BlockState>>> pattern) {
        this.pattern = pattern;
    }

    public List<List<List<BlockState>>> getPattern() {
        return pattern;
    }
}
