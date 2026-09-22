package slimeknights.mantle.recipe.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * {@link Container} extension for a recipe that only needs read access.
 * Used to control which slots an recipe gets and to prevent the need to implement IInventory to get the recipe.
 * Also a {@link RecipeInput}, as recipes take inputs rather than containers since 1.21.
 */
public interface IRecipeContainer extends Container, RecipeInput {
  /* Recipe input */

  @Override
  default int size() {
    return getContainerSize();
  }

  @Override
  default boolean isEmpty() {
    for (int i = 0; i < getContainerSize(); i++) {
      if (!getItem(i).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /* Unsupported operations */

  /** @deprecated unsupported method */
  @Deprecated
  @Override
  default ItemStack removeItem(int index, int count) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated unsupported method */
  @Deprecated
  @Override
  default ItemStack removeItemNoUpdate(int index) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated unsupported method */
  @Deprecated
  @Override
  default void setItem(int index, ItemStack stack) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated unsupported method */
  @Deprecated
  @Override
  default void clearContent() {
    throw new UnsupportedOperationException();
  }

  /* Unused */

  /** @deprecated unused method */
  @Deprecated
  @Override
  default void setChanged() {}

  /** @deprecated unused method */
  @Deprecated
  @Override
  default boolean stillValid(Player player) {
    return true;
  }
}
