package net.petersil98.utilcraft_building.container.architect_table;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

public class BlueprintInventory implements IInventory {

    private final NonNullList<ItemStack> stackList;
    private final int size;

    public BlueprintInventory(int size) {
        this.stackList = NonNullList.withSize(size * size, ItemStack.EMPTY);
        this.size = size;
    }

    @Override
    public int getSizeInventory() {
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
    public ItemStack getStackInSlot(int index) {
        return index >= this.getSizeInventory() ? ItemStack.EMPTY : this.stackList.get(index);
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.stackList, index, count);
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.stackList, index);
    }

    @Override
    public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
        this.stackList.set(index, stack);
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean isUsableByPlayer(@Nonnull PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.stackList.clear();
    }

    public int getSize() {
        return size;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }
}
