package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable for ingredients, handling Forge ingredients */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  private Codec<Ingredient> codec() {
    return this == ALLOW_EMPTY ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY;
  }

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    return codec().parse(JsonOps.INSTANCE, element).getOrThrow(JsonParseException::new);
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    if (object.isEmpty() && this == DISALLOW_EMPTY) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    return codec().encodeStart(JsonOps.INSTANCE, object).getOrThrow(IllegalStateException::new);
  }

  @Override
  public Ingredient decode(FriendlyByteBuf buffer, TypedMap context) {
    if (buffer instanceof RegistryFriendlyByteBuf regBuffer) {
      return Ingredient.CONTENTS_STREAM_CODEC.decode(regBuffer);
    }
    return buffer.readWithCodecTrusted(NbtOps.INSTANCE, codec());
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Ingredient object) {
    if (buffer instanceof RegistryFriendlyByteBuf regBuffer) {
      Ingredient.CONTENTS_STREAM_CODEC.encode(regBuffer, object);
    } else {
      buffer.writeWithCodec(NbtOps.INSTANCE, codec(), object);
    }
  }
}
