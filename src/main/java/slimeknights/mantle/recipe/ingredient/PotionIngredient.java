package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Simple ingredient checking for an item with a specific potion */
public class PotionIngredient extends ItemIngredient {
  /** Ingredient serializer instance */
  public static final IngredientType<PotionIngredient> SERIALIZER = LoadableIngredientSerializer.of(RecordLoadable.create(
    ItemsField.INSTANCE, TAG_FIELD,
    Loadables.POTION.defaultField("potion", Potions.WATER.value(), false, i -> i.potion),
    PotionIngredient::new
  ));

  private final Potion potion;
  protected PotionIngredient(List<Item> items, @Nullable TagKey<Item> itemTag, Potion potion) {
    super(items, itemTag);
    this.potion = potion;
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Potion potion, List<ItemLike> items) {
    return new PotionIngredient(toItem(items), null, potion);
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Potion potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  /** Creates a potion ingredient matching a tag */
  public static PotionIngredient of(Potion potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !super.test(stack)) {
      return false;
    }
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null && contents.potion().isPresent() && contents.potion().get().value() == potion;
  }

  @Override
  public Stream<ItemStack> getItems() {
    return super.getItems().map(stack -> {
      ItemStack s = stack.copy();
      s.set(DataComponents.POTION_CONTENTS, new PotionContents(Holder.direct(potion)));
      return s;
    });
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return SERIALIZER;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PotionIngredient that = (PotionIngredient) o;
    return items.equals(that.items) && Objects.equals(tag, that.tag) && Objects.equals(potion, that.potion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, tag, potion);
  }
}
