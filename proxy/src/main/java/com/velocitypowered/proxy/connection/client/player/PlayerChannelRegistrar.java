package com.velocitypowered.proxy.connection.client.player;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.util.collect.CappedSet;
import java.util.Collection;

public class PlayerChannelRegistrar {
  private static final int MAX_PLUGIN_CHANNELS = 1024;

  private final Collection<String> knownChannels;

  public PlayerChannelRegistrar() {
    this.knownChannels = CappedSet.create(MAX_PLUGIN_CHANNELS);
  }

  /**
   * Return all the plugin message channels "known" to the client.
   * @return the channels
   */
  public Collection<String> getKnownChannels() {
    return knownChannels;
  }

  /**
   * Determines whether or not we can forward a plugin message onto the client.
   * @param version the Minecraft protocol version
   * @param message the plugin message to forward to the client
   * @return {@code true} if the message can be forwarded, {@code false} otherwise
   */
  public boolean canForwardPluginMessage(ProtocolVersion version, PluginMessage message) {
    boolean minecraftOrFmlMessage;

    // By default, all internal Minecraft and Forge channels are forwarded from the server.
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      String channel = message.getChannel();
      minecraftOrFmlMessage = channel.startsWith("MC|")
          || channel.startsWith(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)
          || PluginMessageUtil.isLegacyRegister(message)
          || PluginMessageUtil.isLegacyUnregister(message);
    } else {
      minecraftOrFmlMessage = message.getChannel().startsWith("minecraft:");
    }

    // Otherwise, we need to see if the player already knows this channel or it's known by the
    // proxy.
    return minecraftOrFmlMessage || knownChannels.contains(message.getChannel());
  }

  /**
   * Handles an inbound plugin message.
   * @param message the message to handle
   * @return whether or not we processed the message
   */
  public boolean handlePluginMessage(PluginMessage message) {
    if (PluginMessageUtil.isRegister(message)) {
      this.knownChannels.addAll(PluginMessageUtil.getChannels(message));
    } else if (PluginMessageUtil.isUnregister(message)) {
      this.knownChannels.removeAll(PluginMessageUtil.getChannels(message));
    } else {
      return false;
    }
    return true;
  }
}
