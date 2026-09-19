package slimeknights.mantle.recipe.helper;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.Mantle;

/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible
 * @param <T>  Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
  /** Wraps a StreamCodec with error logging */
  static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, T> loggedStreamCodec(StreamCodec<RegistryFriendlyByteBuf, T> codec) {
    return StreamCodec.of(
      (buffer, recipe) -> {
        try {
          codec.encode(buffer, recipe);
        } catch (RuntimeException e) {
          Mantle.logger.error("Error writing recipe to packet", e);
          throw new EncoderException("Error writing recipe to packet: " + e.getMessage(), e);
        }
      },
      buffer -> {
        try {
          return codec.decode(buffer);
        } catch (RuntimeException e) {
          Mantle.logger.error("Error reading recipe from packet", e);
          throw new DecoderException("Error reading recipe from packet: " + e.getMessage(), e);
        }
      }
    );
  }
}
