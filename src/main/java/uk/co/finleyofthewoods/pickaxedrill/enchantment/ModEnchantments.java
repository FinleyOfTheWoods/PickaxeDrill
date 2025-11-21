package uk.co.finleyofthewoods.pickaxedrill.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEnchantments {
    public static final RegistryKey<Enchantment> DRILL = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of("pickaxedrill", "drill") // namespace, path
    );

    public static void register() {}
}
