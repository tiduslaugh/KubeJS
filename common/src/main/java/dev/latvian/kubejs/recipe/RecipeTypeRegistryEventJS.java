package dev.latvian.kubejs.recipe;

import dev.latvian.kubejs.KubeJS;
import dev.latvian.kubejs.event.EventJS;
import dev.latvian.kubejs.util.UtilsJS;
import dev.latvian.mods.rhino.util.wrap.Wrap;
import me.shedaniel.architectury.registry.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RecipeTypeRegistryEventJS extends EventJS
{
	private final Map<ResourceLocation, RecipeTypeJS> map;

	public RecipeTypeRegistryEventJS(Map<ResourceLocation, RecipeTypeJS> m)
	{
		map = m;
	}

	public void register(RecipeTypeJS type)
	{
		map.put(Registries.getId(type.serializer, Registry.RECIPE_SERIALIZER_REGISTRY), type);
		KubeJS.LOGGER.info("Registered custom recipe handler for type " + type);
	}

	public void register(@Wrap("id") String id, Supplier<RecipeJS> f)
	{
		register(new RecipeTypeJS(Objects.requireNonNull(Registries.get(KubeJS.MOD_ID).get(Registry.RECIPE_SERIALIZER_REGISTRY).get(UtilsJS.getMCID(id)), "Cannot find recipe serializer: " + UtilsJS.getMCID(id)), f));
	}

	public void ignore(@Wrap("id") String id)
	{
		register(new IgnoredRecipeTypeJS(Objects.requireNonNull(Registries.get(KubeJS.MOD_ID).get(Registry.RECIPE_SERIALIZER_REGISTRY).get(UtilsJS.getMCID(id)), "Cannot find recipe serializer: " + UtilsJS.getMCID(id))));
	}
}