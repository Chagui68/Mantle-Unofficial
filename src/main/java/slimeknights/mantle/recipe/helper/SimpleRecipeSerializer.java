package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

/** Simple implementation of a recipe serializer with no properties. */
public class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
  private final MapCodec<T> codec;
  private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

  public SimpleRecipeSerializer(Supplier<T> constructor) {
    this.codec = MapCodec.unit(constructor);
    this.streamCodec = StreamCodec.of((buf, val) -> {}, buf -> constructor.get());
  }

  @Override
  public MapCodec<T> codec() {
    return this.codec;
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
    return this.streamCodec;
  }
}
