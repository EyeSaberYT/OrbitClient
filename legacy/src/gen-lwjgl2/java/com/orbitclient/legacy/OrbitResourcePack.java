package com.orbitclient.legacy;

import net.minecraft.client.resource.ResourceMetadataProvider;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.MetadataSerializer;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * The mod's own assets (cape textures, the white voxel texture) as a resource pack. Legacy Fabric's
 * loader alone doesn't show a mod's assets to the game, and this client doesn't need the rest of
 * Legacy Fabric API, so it brings its one pack itself.
 */
public final class OrbitResourcePack implements ResourcePack {
	public static final OrbitResourcePack INSTANCE = new OrbitResourcePack();

	private OrbitResourcePack() {
	}

	private static String path(Identifier id) {
		return "/assets/" + id.getNamespace() + "/" + id.getPath();
	}

	@Override
	public InputStream open(Identifier id) throws IOException {
		InputStream in = OrbitLegacy.MOD_ID.equals(id.getNamespace()) ? OrbitResourcePack.class.getResourceAsStream(path(id)) : null;
		if (in == null) throw new FileNotFoundException(id.toString());
		return in;
	}

	@Override
	public boolean contains(Identifier id) {
		return OrbitLegacy.MOD_ID.equals(id.getNamespace()) && OrbitResourcePack.class.getResource(path(id)) != null;
	}

	@Override
	public Set<String> getNamespaces() {
		return Collections.singleton(OrbitLegacy.MOD_ID);
	}

	@Override
	public <T extends ResourceMetadataProvider> T parseMetadata(MetadataSerializer serializer, String section) {
		return null;
	}

	@Override
	public BufferedImage getIcon() {
		return null;
	}

	@Override
	public String getName() {
		return "Orbit Client";
	}
}
