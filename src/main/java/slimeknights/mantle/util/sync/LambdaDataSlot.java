package slimeknights.mantle.util.sync;

import lombok.AllArgsConstructor;
import net.minecraft.world.inventory.DataSlot;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Data slot implementation using lambdas for the getter and setter
 */
public class LambdaDataSlot extends DataSlot {
  private final IntSupplier getter;
  private final IntConsumer setter;
  private int lastKnown = 0;
  private final boolean hasCustomStart;

  public LambdaDataSlot(IntSupplier getter, IntConsumer setter) {
    this.getter = getter;
    this.setter = setter;
    this.hasCustomStart = false;
  }

  /** Constructor to let you start from a value other than 0 */
  public LambdaDataSlot(int startingValue, IntSupplier getter, IntConsumer setter) {
    this.getter = getter;
    this.setter = setter;
    this.lastKnown = startingValue;
    this.hasCustomStart = true;
  }

  @Override
  public int get() {
    return getter.getAsInt();
  }

  @Override
  public void set(int value) {
    setter.accept(value);
  }

  @Override
  public boolean checkAndClearUpdateFlag() {
    if (hasCustomStart) {
      int current = this.get();
      boolean changed = current != this.lastKnown;
      this.lastKnown = current;
      return changed;
    }
    return super.checkAndClearUpdateFlag();
  }
}
