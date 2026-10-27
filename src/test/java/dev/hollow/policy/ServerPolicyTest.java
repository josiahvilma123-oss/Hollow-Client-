package dev.hollow.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ServerPolicyTest {
	@Test
	void singleplayerAllowsEverything() {
		assertTrue(ServerPolicy.detectionAllowed(true, null, List.of("*")));
		assertTrue(ServerPolicy.playerDetectionAllowed(true, null, List.of(), List.of()));
	}

	@Test
	void multiplayerPlayersRequireExplicitPermission() {
		assertFalse(ServerPolicy.playerDetectionAllowed(false, "play.example.net", List.of(), List.of()));
		assertTrue(ServerPolicy.playerDetectionAllowed(false, "play.example.net", List.of(), List.of("play.example.net")));
		assertTrue(ServerPolicy.playerDetectionAllowed(false, "Play.Example.Net:25565", List.of(), List.of("play.example.net")));
		assertFalse(ServerPolicy.playerDetectionAllowed(false, "play.example.net:25566", List.of(), List.of("play.example.net")));
	}

	@Test
	void restrictedServersDisableDetection() {
		assertFalse(ServerPolicy.detectionAllowed(false, "mc.strict.org", List.of("*.strict.org")));
		assertFalse(ServerPolicy.playerDetectionAllowed(false, "mc.strict.org", List.of("*.strict.org"), List.of("mc.strict.org")));
		assertTrue(ServerPolicy.detectionAllowed(false, "strict.org.evil.com", List.of("*.strict.org")));
		assertTrue(ServerPolicy.detectionAllowed(false, "other.net", List.of("*.strict.org")));
	}

	@Test
	void portsAreRespected() {
		assertTrue(ServerPolicy.matchesAny("10.0.0.5:25570", List.of("10.0.0.5:25570")));
		assertFalse(ServerPolicy.matchesAny("10.0.0.5:25570", List.of("10.0.0.5")));
		assertTrue(ServerPolicy.matchesAny("10.0.0.5", List.of("10.0.0.5:25565")));
	}

	@Test
	void automationNeedsSingleplayerOrAnExplicitlyPermittedServer() {
		assertTrue(ServerPolicy.automationAllowed(true, "", List.of(), List.of()));
		assertFalse(ServerPolicy.automationAllowed(false, "play.example.net", List.of(), List.of()), "off by default on servers");
		assertTrue(ServerPolicy.automationAllowed(false, "localhost:25570", List.of(), List.of("localhost:25570")));
		assertFalse(ServerPolicy.automationAllowed(false, "mc.strict.org", List.of("*.strict.org"), List.of("mc.strict.org")),
			"restricted servers always win");
	}
}
