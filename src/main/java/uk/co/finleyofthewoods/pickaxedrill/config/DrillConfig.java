package uk.co.finleyofthewoods.pickaxedrill.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DrillConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pickaxedrill.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DrillConfig INSTANCE;

    public boolean debug = false;
    public int blocksPerTick = 10;
    public double durabilityFactor = 0.20;

    public static DrillConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, DrillConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
                INSTANCE = new DrillConfig();
            }
        }  else {
            INSTANCE = new DrillConfig();
            save();
        }
    }
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
