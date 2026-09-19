package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
  private final ShapedRecipeBuilder parent;
  private Ingredient texture = null;
  private char textureKey = '\0';
  private boolean matchAll = false;

  /**
   * Sets the texture source to the given ingredient
   * @param texture Ingredient to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(Ingredient texture) {
    this.texture = texture;
    this.textureKey = '\0';
    return this;
  }

  /**
   * Sets the texture source to the given tag
   * @param tag Tag to use for texture
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setSource(TagKey<Item> tag) {
    return setSource(Ingredient.of(tag));
  }

  /** Sets the texture source to a key from the texture map. */
  public ShapedRetexturedRecipeBuilder setSource(char textureKey) {
    this.textureKey = textureKey;
    this.texture = null;
    return this;
  }

  /**
   * Sets the match first property on the recipe.
   * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  private Ingredient resolveTexture() {
    if (this.texture != null) {
      return this.texture;
    }
    if (this.textureKey != '\0') {
      try {
        for (Field field : ShapedRecipeBuilder.class.getDeclaredFields()) {
          if (Map.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) field.get(this.parent);
            if (map != null && map.containsKey(this.textureKey)) {
              Object val = map.get(this.textureKey);
              if (val instanceof Ingredient ing) {
                return ing;
              }
            }
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to extract ingredient from ShapedRecipeBuilder for key '" + textureKey + "'", e);
      }
    }
    throw new IllegalStateException("No texture defined for texture recipe");
  }

  public void save(RecipeOutput consumer) {
    Ingredient resolvedTexture = resolveTexture();
    parent.save(new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (recipe instanceof ShapedRecipe shaped) {
          consumer.accept(id, new ShapedRetexturedRecipe(shaped, resolvedTexture, matchAll), advancement, conditions);
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

  public void save(RecipeOutput consumer, ResourceLocation location) {
    Ingredient resolvedTexture = resolveTexture();
    parent.save(new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (recipe instanceof ShapedRecipe shaped) {
          consumer.accept(id, new ShapedRetexturedRecipe(shaped, resolvedTexture, matchAll), advancement, conditions);
        } else {
          consumer.accept(id, recipe, advancement, conditions);
        }
      }

      @Override
      public Advancement.Builder advancement() {
        return consumer.advancement();
      }
    }, location);
  }

  public void build(RecipeOutput consumer) {
    save(consumer);
  }

  public void build(RecipeOutput consumer, ResourceLocation location) {
    save(consumer, location);
  }
}
