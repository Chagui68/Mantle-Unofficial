package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.util.RetexturedHelper;

@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe extends ShapedRecipe {
  /** Ingredient used to determine the texture on the output */
  @Getter
  private final Ingredient texture;
  private final boolean matchAll;

  public ShapedRetexturedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, Ingredient texture, boolean matchAll) {
    super(group, category, pattern, result, showNotification);
    this.texture = texture;
    this.matchAll = matchAll;
  }

  public ShapedRetexturedRecipe(ShapedRecipe orig, Ingredient texture, boolean matchAll) {
    super(orig.getGroup(), orig.category(), orig.pattern, orig.getResultItem(null), orig.showNotification());
    this.texture = texture;
    this.matchAll = matchAll;
  }

  public boolean isMatchAll() {
    return matchAll;
  }

  /**
   * Gets the output using the given texture
   * @param texture  Texture to use
   * @return  Output with texture. Will be blank if the input is not a block
   */
  public ItemStack getResultItem(Item texture, HolderLookup.Provider access) {
    return RetexturedHelper.setTexture(getResultItem(access).copy(), Block.byItem(texture));
  }

  @Override
  public ItemStack assemble(CraftingInput craftMatrix, HolderLookup.Provider access) {
    ItemStack result = super.assemble(craftMatrix, access);
    Block currentTexture = null;
    for (int i = 0; i < craftMatrix.size(); i++) {
      ItemStack stack = craftMatrix.getItem(i);
      if (!stack.isEmpty() && texture.test(stack)) {
        // fetch texture from the block if it has one
        Block block = RetexturedHelper.getTexture(stack);
        // assuming it does not, use the block itself as the texture (provided it is not the result that is)
        if (block == Blocks.AIR && stack.getItem() != result.getItem()) {
          block = Block.byItem(stack.getItem());
        }
        // if no texture, skip
        if (block == Blocks.AIR) {
          continue;
        }

        // if we have not found a texture yet, store the found block
        if (currentTexture == null) {
          currentTexture = block;
          // match all means we must check the rest. If not match all, we can be done
          if (!matchAll) {
            break;
          }

          // if we found a texture before, must match or we do no texture
        } else if (currentTexture != block) {
          currentTexture = null;
          break;
        }
      }
    }

    // set the texture if found. No texture will use the fallback
    if (currentTexture != null) {
      return RetexturedHelper.setTexture(result, currentTexture);
    }
    return result;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedRetexturedRecipe> {
    public static final MapCodec<ShapedRetexturedRecipe> CODEC = RecordCodecBuilder.mapCodec(
      inst -> inst.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
        ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.getResultItem(null)),
        Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
        Ingredient.CODEC.fieldOf("texture").forGetter(r -> r.texture),
        Codec.BOOL.optionalFieldOf("match_all", false).forGetter(r -> r.matchAll)
      ).apply(inst, ShapedRetexturedRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRetexturedRecipe> STREAM_CODEC = StreamCodec.of(
      Serializer::toNetwork, Serializer::fromNetwork
    );

    @Override
    public MapCodec<ShapedRetexturedRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ShapedRetexturedRecipe> streamCodec() {
      return STREAM_CODEC;
    }

    private static ShapedRetexturedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      boolean showNotification = buffer.readBoolean();
      Ingredient texture = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
      boolean matchAll = buffer.readBoolean();
      return new ShapedRetexturedRecipe(group, category, pattern, result, showNotification, texture, matchAll);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedRetexturedRecipe recipe) {
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
      ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
      buffer.writeBoolean(recipe.showNotification());
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.texture);
      buffer.writeBoolean(recipe.matchAll);
    }
  }
}
