package slimeknights.mantle.recipe.helper;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/** Helper record to save simple recipes to {@link RecipeOutput}. */
public record SimpleFinishedRecipe(ResourceLocation id, Recipe<?> recipe) {
  public void save(RecipeOutput output) {
    output.accept(id, recipe, null);
  }
}
