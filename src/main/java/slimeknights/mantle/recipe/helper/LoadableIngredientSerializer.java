package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.stream.Stream;

/** Ingredient type made using loadables */
public class LoadableIngredientSerializer {
  private LoadableIngredientSerializer() {}

  public static <T extends ICustomIngredient> IngredientType<T> of(RecordLoadable<T> loadable) {
    return new IngredientType<>(createMapCodec(loadable), createStreamCodec(loadable));
  }

  private static <T extends ICustomIngredient> MapCodec<T> createMapCodec(RecordLoadable<T> loadable) {
    return new MapCodec<T>() {
      @Override
      public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        JsonObject json = new JsonObject();
        loadable.serialize(input, json);
        for (var entry : json.entrySet()) {
          Dynamic<JsonElement> dyn = new Dynamic<>(JsonOps.INSTANCE, entry.getValue());
          prefix.add(entry.getKey(), dyn.convert(ops).getValue());
        }
        return prefix;
      }

      @Override
      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        try {
          Dynamic<O> dyn = new Dynamic<>(ops, ops.createMap(input.entries()));
          JsonElement elem = dyn.convert(JsonOps.INSTANCE).getValue();
          return DataResult.success(loadable.deserialize(elem.getAsJsonObject()));
        } catch (Exception e) {
          return DataResult.error(e::getMessage);
        }
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.empty();
      }
    };
  }

  private static <T extends ICustomIngredient> StreamCodec<RegistryFriendlyByteBuf, T> createStreamCodec(RecordLoadable<T> loadable) {
    return StreamCodec.of(
      (buf, val) -> loadable.encode(buf, val),
      buf -> loadable.decode(buf)
    );
  }
}
