package committee.nova.spawndimension;

import committee.nova.spawndimension.cfg.SpawnDimensionConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class SpawnDimension implements ModInitializer {
    public static SpawnDimensionConfig.Config CONFIG;
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("spawn_dimension.toml").toFile();

    @Override
    public void onInitialize() {
        CONFIG = SpawnDimensionConfig.load(CONFIG_FILE);
    }
}
