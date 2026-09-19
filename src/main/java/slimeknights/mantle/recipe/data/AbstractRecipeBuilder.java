package slimeknights.mantle.recipe.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common logic to create a recipe builder class
 * @param <T>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractRecipeBuilder<T extends AbstractRecipeBuilder<T>> {
  /** Criteria for this recipe */
  protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
  /** Group for this recipe */
  @Nonnull
  protected String group = "";

  /**
   * Adds a criteria to the recipe
   * @param name      Criteria name
   * @param criteria  Criteria instance
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T unlockedBy(String name, Criterion<?> criteria) {
    this.criteria.put(name, criteria);
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe group
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T group(String group) {
    this.group = group;
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe resource location group
   * @return  Builder
   */
  public T group(ResourceLocation group) {
    // if minecraft, no namepsace. Groups are technically not namespaced so this is for consistency with vanilla
    if ("minecraft".equals(group.getNamespace())) {
      return group(group.getPath());
    }
    return group(group.toString());
  }

  /**
   * Builds the recipe with a default recipe ID, typically based on the output
   * @param output  Recipe output
   */
  public abstract void save(RecipeOutput output);

  /**
   * Builds the recipe
   * @param output  Recipe output
   * @param id      Recipe ID
   */
  public abstract void save(RecipeOutput output, ResourceLocation id);

  /**
   * Base logic for advancement building
   * @param output  Recipe output
   * @param id      Recipe ID
   * @return Advancement builder
   */
  private Advancement.Builder buildAdvancementInternal(RecipeOutput output, ResourceLocation id) {
    Advancement.Builder builder = output.advancement()
        .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
        .rewards(AdvancementRewards.Builder.recipe(id))
        .requirements(AdvancementRequirements.Strategy.OR)
        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id));
    this.criteria.forEach(builder::addCriterion);
    return builder;
  }

  /**
   * Builds and validates the advancement, intended to be called in {@link #save(RecipeOutput, ResourceLocation)}
   * @param output  Recipe output
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return AdvancementHolder
   */
  protected AdvancementHolder buildAdvancement(RecipeOutput output, ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      throw new IllegalStateException("No way of obtaining recipe " + id);
    }
    return buildAdvancementInternal(output, id).build(id.withPrefix("recipes/" + folder + "/"));
  }

  /**
   * Builds an optional advancement, intended to be called in {@link #save(RecipeOutput, ResourceLocation)}
   * @param output    Recipe output
   * @param id        Recipe ID
   * @param folder    Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return AdvancementHolder, or null if the advancement was not defined
   */
  @SuppressWarnings("SameParameterValue")  // API
  @Nullable
  protected AdvancementHolder buildOptionalAdvancement(RecipeOutput output, ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      return null;
    }
    return buildAdvancementInternal(output, id).build(id.withPrefix("recipes/" + folder + "/"));
  }
}
