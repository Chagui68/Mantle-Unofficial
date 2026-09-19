package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Condition checking for a combination of tags having any entries
 * @param match  List of tags that the entry must match
 * @param ignore Entries in this tag will be ignored towards the match. If null, all entries are considered
 * @param <T>  Registry type
 */
@SuppressWarnings("unused")
public record TagCombinationCondition<T>(List<TagKey<T>> match, @Nullable TagKey<T> ignore) implements ICondition {
  public static final ResourceLocation ID = Mantle.getResource("tag_combination_filled");

  public static final MapCodec<TagCombinationCondition<?>> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
    ResourceLocation.CODEC.optionalFieldOf("registry", Registries.ITEM.location()).forGetter(c -> c.match().get(0).registry().location()),
    ResourceLocation.CODEC.listOf().fieldOf("match").forGetter(c -> c.match().stream().map(TagKey::location).toList()),
    ResourceLocation.CODEC.optionalFieldOf("ignore").forGetter(c -> Optional.ofNullable(c.ignore()).map(TagKey::location))
  ).apply(builder, (regLoc, matchList, ignoreLoc) -> {
    ResourceKey<? extends Registry<Object>> regKey = ResourceKey.createRegistryKey(regLoc);
    List<TagKey<Object>> matchTags = matchList.stream().map(loc -> TagKey.create(regKey, loc)).toList();
    TagKey<Object> ignoreTag = ignoreLoc.map(loc -> TagKey.create(regKey, loc)).orElse(null);
    return new TagCombinationCondition<>(matchTags, ignoreTag);
  }));

  public TagCombinationCondition {
    if (match.isEmpty()) {
      throw new IllegalArgumentException("Must match at least 1 tag");
    }
  }

  @SafeVarargs
  public static <T> TagCombinationCondition<T> match(@Nullable TagKey<T> ignore, TagKey<T>... match) {
    return new TagCombinationCondition<>(List.of(match), ignore);
  }

  @SafeVarargs
  public static <T> TagCombinationCondition<T> intersection(TagKey<T>... match) {
    return match(null, match);
  }

  public static <T> TagCombinationCondition<T> difference(TagKey<T> match, TagKey<T> ignore) {
    return match(ignore, match);
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    List<Collection<Holder<T>>> tags = match.stream().map(context::getTag).toList();
    Collection<Holder<T>> ignored = ignore == null ? List.of() : context.getTag(ignore);
    if (tags.size() == 1 && ignored.isEmpty()) {
      return !tags.get(0).isEmpty();
    }
    int count = tags.size();
    for (int i = 1; i < count; i++) {
      if (tags.get(i).isEmpty()) {
        return false;
      }
    }

    itemLoop:
    for (Holder<T> entry : tags.get(0)) {
      if (ignored.contains(entry)) {
        continue;
      }
      for (int i = 1; i < count; i++) {
        if (!tags.get(i).contains(entry)) {
          continue itemLoop;
        }
      }
      return true;
    }
    return false;
  }
}
