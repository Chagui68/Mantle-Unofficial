package slimeknights.mantle.recipe.ingredient;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.registration.object.FluidObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient that matches a container of fluid */
@SuppressWarnings("unused")  // API
public class FluidContainerIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = Mantle.getResource("fluid_container");

  public static final MapCodec<FluidContainerIngredient> MAP_CODEC = new MapCodec<>() {
    @Override
    public <T> RecordBuilder<T> encode(FluidContainerIngredient input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      JsonElement element = input.fluidIngredient.serialize();
      Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, element);
      T fluidVal = dynamic.convert(ops).getValue();
      prefix.add("fluid", fluidVal);
      if (input.display != null) {
        prefix.add("display", Ingredient.CODEC.encodeStart(ops, input.display));
      }
      return prefix;
    }

    @Override
    public <T> DataResult<FluidContainerIngredient> decode(DynamicOps<T> ops, MapLike<T> input) {
      T fluidObj = input.get("fluid");
      FluidIngredient fluidIngredient;
      if (fluidObj != null) {
        Dynamic<T> dyn = new Dynamic<>(ops, fluidObj);
        JsonElement elem = dyn.convert(JsonOps.INSTANCE).getValue();
        fluidIngredient = FluidIngredient.LOADABLE.convert(elem, "fluid");
      } else {
        Dynamic<T> dyn = new Dynamic<>(ops, ops.createMap(input.entries()));
        JsonElement elem = dyn.convert(JsonOps.INSTANCE).getValue();
        fluidIngredient = FluidIngredient.LOADABLE.convert(elem, "fluid");
      }
      Ingredient display = null;
      T displayObj = input.get("display");
      if (displayObj != null) {
        display = Ingredient.CODEC.parse(ops, displayObj).result().orElse(null);
      }
      return DataResult.success(new FluidContainerIngredient(fluidIngredient, display));
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.of(ops.createString("fluid"), ops.createString("display"));
    }
  };

  public static final StreamCodec<RegistryFriendlyByteBuf, FluidContainerIngredient> STREAM_CODEC = StreamCodec.of(
    (buffer, ing) -> {
      FluidIngredient.LOADABLE.encode(buffer, ing.fluidIngredient);
      if (ing.display != null) {
        buffer.writeBoolean(true);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ing.display);
      } else {
        buffer.writeBoolean(false);
      }
    },
    buffer -> {
      FluidIngredient fluidIngredient = FluidIngredient.LOADABLE.decode(buffer);
      Ingredient display = null;
      if (buffer.readBoolean()) {
        display = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
      }
      return new FluidContainerIngredient(fluidIngredient, display);
    }
  );

  public static final IngredientType<FluidContainerIngredient> TYPE = new IngredientType<>(MAP_CODEC, STREAM_CODEC);

  /** Ingredient to use for matching */
  private final FluidIngredient fluidIngredient;
  /** Internal ingredient to display the ingredient recipe viewers */
  @Nullable
  private final Ingredient display;

  protected FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
    this.fluidIngredient = fluidIngredient;
    this.display = display;
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
    return new FluidContainerIngredient(ingredient, display);
  }

  /** Creates an instance from a fluid ingredient with no display, not recommended */
  public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient) {
    return new FluidContainerIngredient(ingredient, null);
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static FluidContainerIngredient fromFluid(FluidObject<?> fluid) {
    return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME), Ingredient.of(fluid.asItem()));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    var cap = stack.getCapability(Capabilities.FluidHandler.ITEM);
    if (cap == null || cap.getTanks() != 1) {
      return false;
    }
    FluidStack contained = cap.getFluidInTank(0);
    if (contained.isEmpty() || fluidIngredient.getAmount(contained.getFluid()) != contained.getAmount() || !fluidIngredient.test(contained.getFluid())) {
      return false;
    }
    ItemStack copy = stack.copyWithCount(1);
    var copyCap = copy.getCapability(Capabilities.FluidHandler.ITEM);
    if (copyCap == null) {
      return false;
    }
    Fluid fluid = copyCap.getFluidInTank(0).getFluid();
    int amount = fluidIngredient.getAmount(fluid);
    FluidStack drained = copyCap.drain(amount, FluidAction.EXECUTE);
    return drained.getFluid() == fluid && drained.getAmount() == amount && ItemStack.matches(stack.getCraftingRemainingItem(), copyCap.getContainer());
  }

  @Override
  public Stream<ItemStack> getItems() {
    if (display == null) {
      return Stream.empty();
    }
    return Arrays.stream(display.getItems());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FluidContainerIngredient that = (FluidContainerIngredient) o;
    return Objects.equals(fluidIngredient, that.fluidIngredient) && Objects.equals(display, that.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fluidIngredient, display);
  }
}
