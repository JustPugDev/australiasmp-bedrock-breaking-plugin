package au.com.glob.bedrockBreakingPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public final class PacketListener extends PacketAdapter {
  private final Map<Player, Integer> players = new HashMap<>();
  final int ticksPerStage = Math.round(100 / 9);

  public PacketListener(final BedrockBreakingPlugin plugin) {
    super(plugin, PacketType.Play.Client.BLOCK_DIG);
    this.plugin = plugin;
  }

  @Override
  public void onPacketReceiving(final PacketEvent evt) {
    final Player player = evt.getPlayer();
    if (player == null || !player.isOnline()) {
      return;
    }
    if (player.getGameMode().equals(GameMode.CREATIVE)) {
      return;
    }

    final BlockPosition position = evt.getPacket().getBlockPositionModifier().read(0);
    switch (evt.getPacket().getPlayerDigTypes().read(0)) {
      case ABORT_DESTROY_BLOCK:
      case STOP_DESTROY_BLOCK:
        stopDigging(position, player);
        break;

      case START_DESTROY_BLOCK:
        final Location location = position.toLocation(player.getWorld());

        // chunk must be loaded
        if (!location
            .getWorld()
            .isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
          return;
        }

        // block must be bedrock
        if (!location.getBlock().getType().equals(Material.BEDROCK)) {
          return;
        }

        // player must be holding correct tool
        if (!isHoldingValidTool(player)) {
          return;
        }

        // start breaking animation
        players.put(
            player,
            Bukkit.getScheduler()
                .scheduleSyncRepeatingTask(
                    plugin,
                    new Runnable() {
                      int ticks = 0;

                      @Override
                      public void run() {
                        // check if still online and holding correct tool
                        if (!player.isOnline() || !isHoldingValidTool(player)) {
                          stopDigging(position, player);
                          return;
                        }

                        // bump to next stage
                        ticks += 2;
                        int stage = ticks / ticksPerStage;

                        // haven't been digging long enough
                        if (stage <= 9) {
                          broadcastBlockBreakAnimationPacket(position, stage);
                          return;
                        }

                        // done digging, break block
                        stopDigging(position, player);
                        breakBlock(location.getBlock(), position, player);
                      }
                    },
                    0L,
                    5L));
        break;
      default:
        break;
    }
  }

  private boolean isHoldingValidTool(final Player player) {
    // undamaged netherite pickaxe
    ItemStack tool = player.getInventory().getItemInMainHand();
    Damageable itemMeta = (Damageable) tool.getItemMeta();
    return tool.getType().equals(Material.NETHERITE_PICKAXE)
        && !tool.getItemMeta().isUnbreakable()
        && !itemMeta.hasDamage();
  }

  private void broadcastBlockBreakAnimationPacket(final BlockPosition position, final int stage) {
    final PacketContainer packet =
        ProtocolLibrary.getProtocolManager()
            .createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
    packet.getIntegers().write(0, 0).write(1, stage);
    packet.getBlockPositionModifier().write(0, position);
    ProtocolLibrary.getProtocolManager().broadcastServerPacket(packet);
  }

  private void stopDigging(final BlockPosition position, final Player player) {
    if (!players.containsKey(player)) {
      return;
    }
    Bukkit.getScheduler().cancelTask(players.remove(player));
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            plugin, () -> broadcastBlockBreakAnimationPacket(position, -1), 1L);
  }

  private void breakBlock(final Block block, final BlockPosition position, final Player player) {
    // allow other plugins to cancel breaking
    final BlockBreakEvent breakEvt = new BlockBreakEvent(block, player);
    breakEvt.setDropItems(true);
    Bukkit.getPluginManager().callEvent(breakEvt);
    if (breakEvt.isCancelled()) {
      return;
    }
    if (!breakEvt.isDropItems()) {
      return;
    }

    // break block
    block.breakNaturally(player.getInventory().getItemInMainHand());

    // break pickaxe
    player.getInventory().setItemInMainHand(null);
    player.playSound(
        player.getLocation(),
        Sound.ENTITY_ITEM_BREAK,
        SoundCategory.PLAYERS,
        (float) 1.0,
        (float) 0.9);
  }
}
