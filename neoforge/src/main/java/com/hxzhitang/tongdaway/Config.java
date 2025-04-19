package com.hxzhitang.tongdaway;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@EventBusSubscriber(modid = Tongdaway.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> FEATURES_STRINGS = BUILDER
            .comment("A list of features to ways connect.")
            .comment("路可以连接特征的名单。")
            .comment("example: \"minecraft:village\"")
            .defineListAllowEmpty("features", List.of("minecraft:village"), Config::validateString);

    private static final ModConfigSpec.BooleanValue ALWAYS_CONNECT_VILLAGE = BUILDER
            .comment("Way always connect village?")
            .comment("路是否总是连接村庄？")
            .define("connectVillage", true);

    private static final ModConfigSpec.IntValue CONNECT_FEATURES_NUM = BUILDER
            .comment("A maximum number of features to connect in a region.")
            .comment("一个区域内连接特征的最大数量")
            .defineInRange("connectFeaturesNum", 3, 0, 10);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> NOTES_IN_WAY_SIGN = BUILDER
            .comment("Other information on the way sign.")
            .comment("路牌上的其他信息。")
            .comment("example: \"[HELLO]Welcome to Minecraft World!\"")
            .defineListAllowEmpty("notes", List.of("[HELLO]Welcome to Minecraft World!"), Config::validateString);


    static final ModConfigSpec SPEC = BUILDER.build();

//    public static Set<String> features;
//    public static Set<String> notes;
//    public static boolean alwaysConnectVillage;
//    public static int connectFeaturesNum;

    private static boolean validateString(final Object obj) {
//        return obj instanceof final String itemName && ForgeRegistries.FEATURES.containsKey(new ResourceLocation(itemName));
        return true;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        ConfigVar.alwaysConnectVillage = ALWAYS_CONNECT_VILLAGE.get();
        ConfigVar.connectFeaturesNum = CONNECT_FEATURES_NUM.get();

        // convert the list of strings into a set of items
        ConfigVar.features = new HashSet<>(FEATURES_STRINGS.get());
        ConfigVar.notes = new HashSet<>(NOTES_IN_WAY_SIGN.get());
    }
}
