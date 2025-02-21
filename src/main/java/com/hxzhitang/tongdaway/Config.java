package com.hxzhitang.tongdaway;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Tongdaway.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FEATURES_STRINGS = BUILDER
            .comment("A list of features to ways connect.")
            .comment("路可以连接特征的名单。")
            .comment("example: \"minecraft:village\"")
            .defineListAllowEmpty("features", List.of("minecraft:village"), Config::validateString);

    private static final ForgeConfigSpec.BooleanValue ALWAYS_CONNECT_VILLAGE = BUILDER
            .comment("Way always connect village?")
            .comment("路是否总是连接村庄？")
            .define("connectVillage", true);

    private static final ForgeConfigSpec.IntValue CONNECT_FEATURES_NUM = BUILDER
            .comment("A maximum number of features to connect in a region.")
            .comment("一个区域内连接特征的最大数量")
            .defineInRange("connectFeaturesNum", 3, 0, 10);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> NOTES_IN_WAY_SIGN = BUILDER
            .comment("Other information on the way sign.")
            .comment("路牌上的其他信息。")
            .comment("example: \"[HELLO]Welcome to Minecraft World!\"")
            .defineListAllowEmpty("notes", List.of("[HELLO]Welcome to Minecraft World!"), Config::validateString);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> features;
    public static Set<String> notes;
    public static boolean alwaysConnectVillage;
    public static int connectFeaturesNum;

    private static boolean validateString(final Object obj) {
//        return obj instanceof final String itemName && ForgeRegistries.FEATURES.containsKey(new ResourceLocation(itemName));
        return true;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        alwaysConnectVillage = ALWAYS_CONNECT_VILLAGE.get();
        connectFeaturesNum = CONNECT_FEATURES_NUM.get();

        // convert the list of strings into a set of items
        features = new HashSet<>(FEATURES_STRINGS.get());
        notes = new HashSet<>(NOTES_IN_WAY_SIGN.get());
    }
}
