package com.orbitclient.compat;

//? if <1.19 {
/*import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

// Before 1.19 a missing resource was an exception and a found one opened with getInputStream(). This gives
// Orbit the 1.19 shape (an Optional with open()); build.gradle sends getResource(...) calls here.
public final class Resources {
	private Resources() {
	}

	public interface Opened {
		InputStream open() throws IOException;
	}

	public static Optional<Opened> find(ResourceManager manager, Identifier id) {
		if (!manager.hasResource(id)) return Optional.empty();
		return Optional.of(() -> manager.getResource(id).getInputStream());
	}
}
*///?}
