package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class ShapedFallbackRecipe extends ShapedRecipe {

  /** Recipes to skip if they match */
  private final List<ResourceLocation> alternatives;
  private List<CraftingRecipe> alternativeCache;

  public ShapedFallbackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<ResourceLocation> alternatives) {
    super(group, category, pattern, result, showNotification);
    this.alternatives = alternatives;
  }

  public ShapedFallbackRecipe(ShapedRecipe base, List<ResourceLocation> alternatives) {
    super(base.getGroup(), base.category(), base.pattern, base.getResultItem(null), base.showNotification());
    this.alternatives = alternatives;
  }

  public List<ResourceLocation> getAlternatives() {
    return alternatives;
  }

  @Override
  public boolean matches(CraftingInput inv, Level world) {
    if (!super.matches(inv, world)) {
      return false;
    }

    if (alternativeCache == null) {
      RecipeManager manager = world.getRecipeManager();
      alternativeCache = alternatives.stream()
                                     .map(manager::byKey)
                                     .filter(Optional::isPresent)
                                     .map(opt -> opt.get().value())
                                     .filter(recipe -> {
                                       Class<?> clazz = recipe.getClass();
                                       return clazz == ShapedRecipe.class || clazz == ShapelessRecipe.class;
                                     })
                                     .map(recipe -> (CraftingRecipe) recipe).collect(Collectors.toList());
    }
    return this.alternativeCache.stream().noneMatch(recipe -> recipe.matches(inv, world));
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedFallbackRecipe> {
    public static final MapCodec<ShapedFallbackRecipe> CODEC = RecordCodecBuilder.mapCodec(
      inst -> inst.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
        ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.getResultItem(null)),
        Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
        ResourceLocation.CODEC.listOf().fieldOf("alternatives").forGetter(ShapedFallbackRecipe::getAlternatives)
      ).apply(inst, ShapedFallbackRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.of(
      Serializer::toNetwork, Serializer::fromNetwork
    );

    @Override
    public MapCodec<ShapedFallbackRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ShapedFallbackRecipe> streamCodec() {
      return STREAM_CODEC;
    }

    private static ShapedFallbackRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      boolean showNotification = buffer.readBoolean();
      List<ResourceLocation> alternatives = buffer.readList(ResourceLocation.STREAM_CODEC);
      return new ShapedFallbackRecipe(group, category, pattern, result, showNotification, alternatives);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedFallbackRecipe recipe) {
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
      ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
      buffer.writeBoolean(recipe.showNotification());
      buffer.writeCollection(recipe.alternatives, (buf, loc) -> ResourceLocation.STREAM_CODEC.encode(buf, loc));
    }
  }
}
