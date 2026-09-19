package slimeknights.mantle.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ingredient for a non-NBT sensitive item from another mod, should never be used outside datagen
 */
public class ItemNameIngredient implements ICustomIngredient {
  public static final MapCodec<ItemNameIngredient> CODEC = RecordCodecBuilder.mapCodec(
    inst -> inst.group(
      ResourceLocation.CODEC.listOf().fieldOf("items").forGetter(i -> i.names)
    ).apply(inst, ItemNameIngredient::new)
  );
  public static final IngredientType<ItemNameIngredient> TYPE = new IngredientType<>(CODEC);

  private final List<ResourceLocation> names;

  public ItemNameIngredient(List<ResourceLocation> names) {
    this.names = names;
  }

  /** Creates a new ingredient from a list of names */
  public static Ingredient from(List<ResourceLocation> names) {
    return new ItemNameIngredient(names).toVanilla();
  }

  /** Creates a new ingredient from a list of names */
  public static Ingredient from(ResourceLocation... names) {
    return from(Arrays.asList(names));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return names.contains(key);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return names.stream()
      .map(BuiltInRegistries.ITEM::get)
      .filter(item -> item != null && item != Items.AIR)
      .map(ItemStack::new);
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
    ItemNameIngredient that = (ItemNameIngredient) o;
    return names.equals(that.names);
  }

  @Override
  public int hashCode() {
    return names.hashCode();
  }
}
