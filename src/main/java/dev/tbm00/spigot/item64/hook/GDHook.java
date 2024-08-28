package dev.tbm00.spigot.item64.hook;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.lib.flowpowered.math.vector.Vector3i;

public class GDHook {
	private final List<String> ignoredClaims;

	public GDHook(JavaPlugin javaPlugin) {
		ConfigurationSection gdSection = javaPlugin.getConfig().getConfigurationSection("hooks.GriefDefender");
		if (gdSection != null) {
			ignoredClaims = gdSection.getStringList("ignoredClaims");
			if (ignoredClaims != null && !ignoredClaims.isEmpty()) javaPlugin.getLogger().info("ignoredClaims: " + ignoredClaims);
		} else ignoredClaims = null;
	}
	
	public String getRegionID(Location location) {
		final Vector3i vector = Vector3i.from(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		final Core gd = GriefDefender.getCore();
		final Claim claim = gd.getClaimManager(location.getWorld().getUID()).getClaimAt(vector);
		return !claim.isWilderness() ? claim.getUniqueId().toString() : null;
	}

	public boolean hasBuilderTrust(OfflinePlayer player, String regionID) {
		if (regionID == null || regionID.isEmpty()) return true;
		if (ignoredClaims.contains(regionID)) return true;
		final Core gd = GriefDefender.getCore();
		final Claim claim = gd.getClaim(UUID.fromString(regionID));
		if (claim == null) return true;
		if (claim.getUserTrusts(TrustTypes.MANAGER).contains(player.getUniqueId())
		|| claim.getUserTrusts(TrustTypes.BUILDER).contains(player.getUniqueId()))
			return true;
		return player.getUniqueId().toString().equals(claim.getOwnerUniqueId().toString());
	}

	public boolean hasPvPEnabled(String regionID) {
		if (regionID == null || regionID.isEmpty()) return true;
		if (ignoredClaims.contains(regionID)) return true;
		final Core gd = GriefDefender.getCore();
		final Claim claim = gd.getClaim(UUID.fromString(regionID));
		if (claim == null) return true;
		return claim.isPvpAllowed();	
	}
}