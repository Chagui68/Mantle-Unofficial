package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builder for a shaped recipe with fallbacks */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fallback")
public class ShapedFallbackRecipeBuilder {
  private final ShapedRecipeBuilder base;
  private final List<ResourceLocation> alternatives = new ArrayList<>();

  /**
   * Adds a single alternative to this recipe. Any matching alternative causes this recipe to fail
   * @param location  Alternative
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternative(ResourceLocation location) {
    this.alternatives.add(location);
    return this;
  }

  /**
   * Adds a list of alternatives to this recipe. Any matching alternative causes this recipe to fail
   * @param locations  Alternative list
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternatives(Collection<ResourceLocation> locations) {
    this.alternatives.addAll(locations);
    return this;
  }

  /**
   * Builds the recipe using the output as the name
   * @param consumer  Recipe consumer
   */
  public void save(RecipeOutput consumer) {
    base.save(new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (recipe instanceof ShapedRecipe shaped) {
          consumer.accept(id, new ShapedFallbackRecipe(shaped, alternatives), advancement, conditions);
        } else {
          consumer.accept(id, recipe, advancement, conditions);
        }
      }

      @Override
      public Advancement.Builder advancement() {
        return consumer.advancement();
      }
    });
  }

  /**
   * Builds the recipe using the given ID
   * @param consumer  Recipe consumer
   * @param id        Recipe ID
   */
  public void save(RecipeOutput consumer, ResourceLocation id) {
    base.save(new RecipeOutput() {
      @Override
      public void accept(ResourceLocation recipeId, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (recipe instanceof ShapedRecipe shaped) {
          consumer.accept(recipeId, new ShapedFallbackRecipe(shaped, alternatives), advancement, conditions);
        } else {
          consumer.accept(recipeId, recipe, advancement, conditions);
        }
      }

      @Override
      public Advancement.Builder advancement() {
        return consumer.advancement();
      }
    }, id);
  }

  public void build(RecipeOutput consumer) {
    save(consumer);
  }

  public void build(RecipeOutput consumer, ResourceLocation id) {
    save(consumer, id);
  }
}
