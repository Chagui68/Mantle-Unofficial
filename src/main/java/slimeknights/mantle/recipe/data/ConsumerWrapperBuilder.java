package slimeknights.mantle.recipe.data;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a recipe consumer wrapper, which adds some extra properties to wrap the result of another recipe
 */
@SuppressWarnings("unused")  // API
public class ConsumerWrapperBuilder {
  private final List<ICondition> conditions = new ArrayList<>();
  @Nullable
  private final RecipeSerializer<?> override;
  @Nullable
  private final ResourceLocation overrideName;

  private ConsumerWrapperBuilder(@Nullable RecipeSerializer<?> override, @Nullable ResourceLocation overrideName) {
    this.override = override;
    this.overrideName = overrideName;
  }

  public static ConsumerWrapperBuilder wrap() {
    return new ConsumerWrapperBuilder(null, null);
  }

  public static ConsumerWrapperBuilder wrap(RecipeSerializer<?> override) {
    return new ConsumerWrapperBuilder(override, null);
  }

  public static ConsumerWrapperBuilder wrap(ResourceLocation override) {
    return new ConsumerWrapperBuilder(null, override);
  }

  @CanIgnoreReturnValue
  public ConsumerWrapperBuilder addCondition(ICondition condition) {
    conditions.add(condition);
    return this;
  }

  public RecipeOutput build(RecipeOutput consumer) {
    if (conditions.isEmpty()) {
      return consumer;
    }
    return consumer.withConditions(conditions.toArray(new ICondition[0]));
  }
}
