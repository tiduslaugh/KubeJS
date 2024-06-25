package dev.latvian.mods.kubejs.ingredient;

import dev.latvian.mods.kubejs.item.ItemStackJS;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface KubeJSIngredient extends ICustomIngredient, Predicate<ItemStack> {
	@Override
	boolean test(ItemStack stack);

	@Override
	default Stream<ItemStack> getItems() {
		return ItemStackJS.getList().stream().filter(this);
	}

	@Override
	default boolean isSimple() {
		return true;
	}
}
