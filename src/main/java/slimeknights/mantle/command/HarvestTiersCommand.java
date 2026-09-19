package slimeknights.mantle.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Command to dump harvest tiers */
public class HarvestTiersCommand {
  protected static final ResourceLocation HARVEST_TIERS = ResourceLocation.fromNamespaceAndPath("neoforge", "item_tier_ordering.json");
  private static final String HARVEST_TIER_PATH = HARVEST_TIERS.getNamespace() + "/" + HARVEST_TIERS.getPath();

  private static final Component SUCCESS_LOG = Component.translatable("command.mantle.harvest_tiers.success_log");
  private static final Component EMPTY = Component.translatable("command.mantle.tag.empty");

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(sender -> sender.hasPermission(MantleCommand.PERMISSION_EDIT_SPAWN))
              .then(Commands.literal("save").executes(source -> run(source, true)))
              .then(Commands.literal("log").executes(source -> run(source, false)))
              .then(Commands.literal("list").executes(HarvestTiersCommand::list));
  }

  /** Creates a clickable component for a block tag */
  private static Object getTagComponent(TagKey<Block> tag) {
    ResourceLocation id = tag.location();
    return Component.literal(id.toString()).withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/mantle dump_tag " + Registries.BLOCK.location() + " " + id + " save")));
  }

  /** Runs the command, dumping the tag */
  private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    List<Tiers> sortedTiers = List.of(Tiers.values());

    MutableComponent output = Component.translatable("command.mantle.harvest_tiers.success_list");
    if (sortedTiers.isEmpty()) {
      output.append("\n* ").append(EMPTY);
    } else {
      for (Tiers tier : sortedTiers) {
        output.append("\n* ");
        TagKey<Block> tag = tier.getIncorrectBlocksForDrops();
        String id = tier.name().toLowerCase();
        if (tag != null) {
          output.append(Component.translatable("command.mantle.harvest_tiers.tag", id, getTagComponent(tag)));
        } else {
          output.append(Component.translatable("command.mantle.harvest_tiers.no_tag", id));
        }
      }
    }
    context.getSource().sendSuccess(() -> output, true);
    return sortedTiers.size();
  }

  /** Runs the command, dumping the tag */
  private static int run(CommandContext<CommandSourceStack> context, boolean saveFile) throws CommandSyntaxException {
    List<Tiers> sortedTiers = List.of(Tiers.values());

    JsonArray entries = new JsonArray();
    for (Tiers tier : sortedTiers) {
      entries.add(tier.name().toLowerCase());
    }
    JsonObject json = new JsonObject();
    json.add("order", entries);

    if (saveFile) {
      File output = new File(DumpAllTagsCommand.getOutputFile(context), HARVEST_TIER_PATH);
      Path path = output.toPath();
      try {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
          writer.write(DumpTagCommand.GSON.toJson(json));
        }
      } catch (IOException ex) {
        Mantle.logger.error("Couldn't save harvests tiers to {}", path, ex);
      }
      context.getSource().sendSuccess(() -> Component.translatable("command.mantle.harvest_tiers.success_save", GeneratePackHelper.getOutputComponent(output)), true);
    } else {
      context.getSource().sendSuccess(() -> SUCCESS_LOG, true);
      Mantle.logger.info("Dump of harvests tiers:\n{}", DumpTagCommand.GSON.toJson(json));
    }
    return sortedTiers.size();
  }
}
