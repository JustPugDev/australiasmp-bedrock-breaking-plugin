package au.com.glob.bedrockBreakingPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.plugin.java.JavaPlugin;

public class BedrockBreakingPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
  }

  @Override
  public void onDisable() {
    ProtocolLibrary.getProtocolManager().removePacketListeners(this);
  }
}
