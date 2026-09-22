package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Recipe serializer instance using loadables.
 * @param <T>  Recipe type
 */
public class LoadableRecipeSerializer<T extends Recipe<?>> implements LoggingRecipeSerializer<T> {
  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP = StringLoadable.DEFAULT.defaultField("group", "", Recipe::getGroup);

  protected final RecordLoadable<T> loadable;
  protected final MapCodec<T> codec;
  protected final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;
  /** Context passed to the loadable, built lazily as the recipe type is not available during construction */
  @Nullable
  private TypedMap context;

  protected LoadableRecipeSerializer(RecordLoadable<T> loadable) {
    this.loadable = loadable;
    this.codec = new MapCodec<T>() {
      @Override
      public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        try {
          JsonObject json = new JsonObject();
          loadable.serialize(input, json);
          for (var entry : json.entrySet()) {
            Dynamic<JsonElement> dyn = new Dynamic<>(JsonOps.INSTANCE, entry.getValue());
            prefix.add(entry.getKey(), dyn.convert(ops).getValue());
          }
        } catch (Exception e) {
          Mantle.logger.error("Error encoding recipe with loadable {}", loadable, e);
        }
        return prefix;
      }

      @Override
      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        try {
          Dynamic<O> dyn = new Dynamic<>(ops, ops.createMap(input.entries()));
          JsonElement elem = dyn.convert(JsonOps.INSTANCE).getValue();
          return DataResult.success(loadable.deserialize(elem.getAsJsonObject(), context()));
        } catch (Exception e) {
          return DataResult.error(e::getMessage);
        }
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.empty();
      }
    };
    this.streamCodec = StreamCodec.of(
      (buf, val) -> loadable.encode(buf, val),
      buf -> loadable.decode(buf, context())
    );
  }

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return new LoadableRecipeSerializer<>(loadable);
  }

  /** Creates a type aware serializer from a loadable */
  public static <T extends R, R extends Recipe<?>> TypeAwareRecipeSerializer<T> of(RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return new TypeAware<>(loadable, type);
  }

  /** Creates a serializer that is deprecated, logging a warning when used */
  public static <T extends Recipe<?>> RecipeSerializer<T> deprecated(RecordLoadable<T> loadable, String replacement) {
    return new Deprecated<>(loadable, replacement);
  }

  /** Gets the context for this serializer, containing the serializer itself so loadables can request it */
  protected TypedMap context() {
    if (context == null) {
      context = buildContext(TypedMapBuilder.builder().put(SERIALIZER, this)).build();
    }
    return context;
  }

  /** Adds any serializer specific keys to the context */
  protected TypedMapBuilder buildContext(TypedMapBuilder builder) {
    return builder;
  }

  @Override
  public MapCodec<T> codec() {
    return codec;
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
    return streamCodec;
  }

  public static class TypeAware<T extends Recipe<?>> extends LoadableRecipeSerializer<T> implements TypeAwareRecipeSerializer<T> {
    private final Supplier<? extends RecipeType<?>> type;
    protected TypeAware(RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type) {
      super(loadable);
      this.type = type;
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }

    @Override
    protected TypedMapBuilder buildContext(TypedMapBuilder builder) {
      return builder.put(TYPED_SERIALIZER, this).put(TYPE, getType());
    }
  }

  /** Helper class that logs a warning on recipe parse about planned removal */
  private static class Deprecated<T extends Recipe<?>> extends LoadableRecipeSerializer<T> {
    private final String replacement;
    protected Deprecated(RecordLoadable<T> loadable, String replacement) {
      super(loadable);
      this.replacement = replacement;
    }
  }
}
