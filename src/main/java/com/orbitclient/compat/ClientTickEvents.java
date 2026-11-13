package com.orbitclient.compat;

//? if <1.16.1 {
/*import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// Minecraft 1.16 itself: the only Fabric API that loads on it has no client tick event yet, so Orbit brings
// its own with the same shape (fired by OwnEventsMixin). build.gradle points the import here.
public final class ClientTickEvents {
	public static final class Event {
		private final List<Consumer<Minecraft>> listeners = new CopyOnWriteArrayList<>();

		public void register(Consumer<Minecraft> listener) {
			listeners.add(listener);
		}

		public void fire(Minecraft client) {
			for (Consumer<Minecraft> listener : listeners) listener.accept(client);
		}
	}

	public static final Event END_CLIENT_TICK = new Event();

	private ClientTickEvents() {
	}
}
*///?}
