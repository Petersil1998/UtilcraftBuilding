package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.*;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

public class Recipes extends RecipeProvider {

    public Recipes(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildCraftingRecipes(@Nonnull Consumer<FinishedRecipe> consumer) {
        registerShapedRecipes(consumer);
        registerShapelessRecipes(consumer);
        registerSmeltingRecipes(consumer);
        registerSushiMakerRecipes(consumer);
    }

    private void registerShapedRecipes(Consumer<FinishedRecipe> consumer) {
    }

    private void registerShapelessRecipes(Consumer<FinishedRecipe> consumer) {
    }

    private void registerSmeltingRecipes(Consumer<FinishedRecipe> consumer) {
    }

    private void registerSushiMakerRecipes(Consumer<FinishedRecipe> consumer) {

    }
}