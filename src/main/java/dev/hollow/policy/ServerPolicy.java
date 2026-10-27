package dev.hollow.policy;

import java.util.Collection;
import java.util.Locale;

/**
 * Decides which detection and automation features may run on the current server. Single-player worlds
 * allow everything. On multiplayer servers, player detection is off unless the user has
 * explicitly listed the server as one where it is permitted, and every detection feature
 * shuts off on servers the user marked as restricted.
 */
public final class ServerPolicy {
	private ServerPolicy() {
	}

	public static boolean detectionAllowed(boolean singleplayer, String address, Collection<String> restrictedServers) {
		return singleplayer || !matchesAny(address, restrictedServers);
	}

	public static boolean playerDetectionAllowed(boolean singleplayer, String address, Collection<String> restrictedServers,
			Collection<String> permittedServers) {
		if (singleplayer) {
			return true;
		}
		return detectionAllowed(false, address, restrictedServers) && matchesAny(address, permittedServers);
	}

	/**
	 * Automated play (aim assistance, automatic attacks and block placement) acts in
	 * single-player, and on multiplayer only on servers the user explicitly listed as permitting
	 * it. Restricted servers always win.
	 */
	public static boolean automationAllowed(boolean singleplayer, String address, Collection<String> restrictedServers,
			Collection<String> permittedServers) {
		if (singleplayer) {
			return true;
		}
		return detectionAllowed(false, address, restrictedServers) && matchesAny(address, permittedServers);
	}

	/**
	 * Matches "host", "host:port" and wildcard "*.example.net" entries against an address.
	 * The default port 25565 is implied when an entry has no port.
	 */
	public static boolean matchesAny(String address, Collection<String> entries) {
		if (address == null || address.isBlank()) {
			return false;
		}
		String normalized = normalize(address);
		String host = hostOf(normalized);
		for (String raw : entries) {
			String entry = normalize(raw);
			if (entry.isEmpty()) {
				continue;
			}
			if (entry.startsWith("*.")) {
				String suffix = entry.substring(1);
				String entryHost = hostOf(suffix);
				if (host.endsWith(entryHost) && host.length() > entryHost.length()) {
					return true;
				}
				continue;
			}
			if (entry.equals(normalized) || entry.equals(host) && portOf(normalized) == 25565) {
				return true;
			}
		}
		return false;
	}

	public static String normalize(String address) {
		String trimmed = address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
		if (trimmed.endsWith(".")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		if (trimmed.endsWith(":25565")) {
			trimmed = trimmed.substring(0, trimmed.length() - ":25565".length());
		}
		return trimmed;
	}

	private static String hostOf(String address) {
		int colon = address.lastIndexOf(':');
		return colon >= 0 && address.indexOf(':') == colon ? address.substring(0, colon) : address;
	}

	private static int portOf(String address) {
		int colon = address.lastIndexOf(':');
		if (colon < 0 || address.indexOf(':') != colon) {
			return 25565;
		}
		try {
			return Integer.parseInt(address.substring(colon + 1));
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
