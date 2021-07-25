package net.petersil98.utilcraft_building.container.architect_table;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import javax.annotation.Nonnull;

public class BlueprintInventory implements Container {

    private final NonNullList<ItemStack> stackList;
    private final int size;

    public BlueprintInventory(int size) {
        this.stackList = NonNullList.withSize(size * size, ItemStack.EMPTY);
        this.size = size;
    }

    @Override
    public int getContainerSize() {
        return this.stackList.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.stackList) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem(int index) {
        return index >= this.getContainerSize() ? ItemStack.EMPTY : this.stackList.get(index);
    }

    @Nonnull
    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(this.stackList, index, count);
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.stackList, index);
    }

    @Override
    public void setItem(int index, @Nonnull ItemStack stack) {
        this.stackList.set(index, stack);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.stackList.clear();
    }

    public int getSize() {
        return size;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
