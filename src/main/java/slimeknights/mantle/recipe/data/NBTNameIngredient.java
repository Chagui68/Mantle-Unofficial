package slimeknights.mantle.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Ingredient for a NBT sensitive item from another mod, should never be used outside datagen
 */
public class NBTNameIngredient implements ICustomIngredient {
  public static final MapCodec<NBTNameIngredient> CODEC = RecordCodecBuilder.mapCodec(
    inst -> inst.group(
      ResourceLocation.CODEC.fieldOf("item").forGetter(i -> i.name),
      CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(i -> Optional.ofNullable(i.nbt))
    ).apply(inst, (name, nbt) -> new NBTNameIngredient(name, nbt.orElse(null)))
  );
  public static final IngredientType<NBTNameIngredient> TYPE = new IngredientType<>(CODEC);

  private final ResourceLocation name;
  @Nullable
  private final CompoundTag nbt;

  protected NBTNameIngredient(ResourceLocation name, @Nullable CompoundTag nbt) {
    this.name = name;
    this.nbt = nbt;
  }

  /**
   * Creates an ingredient for the given name and NBT
   * @param name  Item name
   * @param nbt   NBT
   * @return  Ingredient
   */
  public static Ingredient from(ResourceLocation name, CompoundTag nbt) {
    return new NBTNameIngredient(name, nbt).toVanilla();
  }

  /**
   * Creates an ingredient for an item that must have no NBT
   * @param name  Item name
   * @return  Ingredient
   */
  public static Ingredient from(ResourceLocation name) {
    return new NBTNameIngredient(name, null).toVanilla();
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return name.equals(key);
  }

  @Override
  public Stream<ItemStack> getItems() {
    Item item = BuiltInRegistries.ITEM.get(name);
    if (item != null && item != Items.AIR) {
      return Stream.of(new ItemStack(item));
    }
    return Stream.empty();
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
    NBTNameIngredient that = (NBTNameIngredient) o;
    return Objects.equals(name, that.name) && Objects.equals(nbt, that.nbt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, nbt);
  }
}
