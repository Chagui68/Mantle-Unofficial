package slimeknights.mantle.plugin.jei;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JEI crafting extension to properly show, animate, and focus {@link ShapedRetexturedRecipe} instances
 */
public class RetexturableRecipeExtension implements ICraftingCategoryExtension<ShapedRetexturedRecipe> {

  public RetexturableRecipeExtension() {}

  /** Checks if two ingredients match based on their display items */
  private static boolean ingredientsMatch(Ingredient left, Ingredient right) {
    ItemStack[] leftStacks = left.getItems();
    ItemStack[] rightStacks = right.getItems();
    if (leftStacks.length != rightStacks.length) {
      return false;
    }
    for (int i = 0; i < leftStacks.length; i++) {
      if (!ItemStack.isSameItemSameComponents(leftStacks[i], rightStacks[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Optional<ResourceLocation> getRegistryName(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return Optional.of(holder.id());
  }

  @Override
  public int getWidth(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedRetexturedRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ShapedRetexturedRecipe recipe = holder.value();

    // set the output to display all variants from the texture ingredient
    Ingredient texture = recipe.getTexture();
    // fetch all stacks from the ingredient, note any variants that are not blocks will get a blank look
    RegistryAccess access = Objects.requireNonNull(SafeClientAccess.getRegistryAccess());
    List<ItemStack> displayOutputs = Arrays.stream(texture.getItems())
                                           .map(stack -> recipe.getResultItem(stack.getItem(), access))
                                           .toList();
    // empty display means the tag found nothing, so just use the original output
    List<ItemStack> outputs = displayOutputs.isEmpty() ? List.of(recipe.getResultItem(access)) : displayOutputs;

    // find out which inputs match the texture, we will need to use those for the focus link
    List<Ingredient> inputsList = recipe.getIngredients();
    int[] textureSlots = IntStream.range(0, inputsList.size()).filter(i -> ingredientsMatch(texture, inputsList.get(i))).toArray();

    // we need the blank version for the sake of recipe lookup due to the subtype interpreter making it not the same
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.getResultItem(access));

    // add the itemstacks to the grid
    List<List<ItemStack>> inputStacks = recipe.getIngredients().stream().map(ingredient -> List.of(ingredient.getItems())).toList();
    int width = recipe.getWidth();
    int height = recipe.getHeight();
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, outputs);
    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3", holder.id(), builder.getClass().getName());
    } else {
      // link the output to all inputs that match the texture
      builder.createFocusLink(Streams.concat(Stream.of(output), Arrays.stream(textureSlots).mapToObj(i -> inputs.get(MantleJEIConstants.getCraftingIndex(i, width, height)))).toArray(IRecipeSlotBuilder[]::new));
    }
  }
}
