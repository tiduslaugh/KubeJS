package dev.latvian.mods.kubejs.recipe.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilder;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecipeSchemaStorage {
	public final Map<ResourceLocation, KubeRecipeFactory> recipeTypes;
	public final Map<String, RecipeNamespace> namespaces;
	public final Map<String, ResourceLocation> mappings;
	public final Map<String, RecipeComponent<?>> simpleComponents;
	public final Map<String, RecipeComponentFactory> dynamicComponents;
	private final Map<String, RecipeComponent<?>> componentCache;
	public final Map<String, RecipeSchemaType> schemaTypes;
	public RecipeSchema shapedSchema;
	public RecipeSchema shapelessSchema;
	public RecipeSchema specialSchema;

	public RecipeSchemaStorage() {
		this.recipeTypes = new HashMap<>();
		this.namespaces = new HashMap<>();
		this.mappings = new HashMap<>();
		this.simpleComponents = new HashMap<>();
		this.dynamicComponents = new HashMap<>();
		this.componentCache = new HashMap<>();
		this.schemaTypes = new HashMap<>();
	}

	public RecipeNamespace namespace(String namespace) {
		return namespaces.computeIfAbsent(namespace, n -> new RecipeNamespace(this, n));
	}

	public void fireEvents(RegistryAccessContainer registries, ResourceManager resourceManager) {
		recipeTypes.clear();
		namespaces.clear();
		mappings.clear();
		simpleComponents.clear();
		dynamicComponents.clear();
		componentCache.clear();
		schemaTypes.clear();
		shapedSchema = null;
		shapelessSchema = null;
		specialSchema = null;

		var typeEvent = new RecipeFactoryRegistry(this);
		KubeJSPlugins.forEachPlugin(typeEvent, KubeJSPlugin::registerRecipeFactories);

		for (var entry : resourceManager.listResources("kubejs", path -> path.getPath().endsWith("/recipe_mappings.json")).entrySet()) {
			try (var reader = entry.getValue().openAsReader()) {
				var json = JsonUtils.GSON.fromJson(reader, JsonObject.class);

				for (var entry1 : json.entrySet()) {
					var id = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), entry1.getKey());

					if (entry1.getValue() instanceof JsonArray arr) {
						for (var n : arr) {
							mappings.put(n.getAsString(), id);
						}
					} else {
						mappings.put(entry1.getValue().getAsString(), id);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		var mappingRegistry = new RecipeMappingRegistry(this);
		KubeJSPlugins.forEachPlugin(mappingRegistry, KubeJSPlugin::registerRecipeMappings);
		ServerEvents.RECIPE_MAPPING_REGISTRY.post(ScriptType.SERVER, mappingRegistry);

		KubeJSPlugins.forEachPlugin(new RecipeComponentFactoryRegistry(this), KubeJSPlugin::registerRecipeComponents);

		for (var entry : resourceManager.listResources("kubejs", path -> path.getPath().endsWith("/recipe_components.json")).entrySet()) {
			try (var reader = entry.getValue().openAsReader()) {
				var json = JsonUtils.GSON.fromJson(reader, JsonObject.class);

				for (var entry1 : json.entrySet()) {
					simpleComponents.put(entry1.getKey(), getComponent(registries, entry1.getValue().getAsString()));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		for (var entry : BuiltInRegistries.RECIPE_SERIALIZER.entrySet()) {
			var ns = namespace(entry.getKey().location().getNamespace());
			ns.put(entry.getKey().location().getPath(), new UnknownRecipeSchemaType(ns, entry.getKey().location(), entry.getValue()));
		}

		var schemaRegistry = new RecipeSchemaRegistry(this);
		JsonRecipeSchemaLoader.load(this, registries, schemaRegistry, resourceManager);

		shapedSchema = Objects.requireNonNull(namespace("minecraft").get("shaped").schema);
		shapelessSchema = Objects.requireNonNull(namespace("minecraft").get("shapeless").schema);
		specialSchema = Objects.requireNonNull(namespace("minecraft").get("special").schema);

		KubeJSPlugins.forEachPlugin(schemaRegistry, KubeJSPlugin::registerRecipeSchemas);
		ServerEvents.RECIPE_SCHEMA_REGISTRY.post(ScriptType.SERVER, schemaRegistry);
	}

	public RecipeComponent<?> getComponent(RegistryAccessContainer registries, String string) {
		var c = componentCache.get(string);

		if (c == null) {
			try {
				c = readComponent(registries, new StringReader(string));
				componentCache.put(string, c);
			} catch (Exception ex) {
				throw new IllegalArgumentException(ex);
			}
		}

		return c;
	}

	public RecipeComponent<?> readComponent(RegistryAccessContainer registries, StringReader reader) throws Exception {
		reader.skipWhitespace();

		if (!reader.canRead()) {
			throw new IllegalArgumentException("Nothing to read");
		}

		if (reader.peek() == '{') {
			reader.skip();

			var keys = new ArrayList<RecipeComponentBuilder.Key>();

			while (true) {
				reader.skipWhitespace();

				if (!reader.canRead()) {
					throw new IllegalArgumentException("Expected key name");
				}

				var name = reader.readString();
				var optional = false;
				var alwaysWrite = false;

				reader.skipWhitespace();

				if (reader.canRead() && reader.peek() == '?') {
					reader.skip();
					reader.skipWhitespace();
					optional = true;
				}

				if (reader.canRead() && reader.peek() == '!') {
					reader.skip();
					reader.skipWhitespace();
					alwaysWrite = true;
				}

				reader.expect(':');
				reader.skipWhitespace();

				var component = readComponent(registries, reader);

				keys.add(new RecipeComponentBuilder.Key(name, component, optional, alwaysWrite));

				reader.skipWhitespace();

				if (!reader.canRead()) {
					throw new IllegalArgumentException("Unexpected EOL");
				} else if (reader.peek() == ',') {
					reader.skip();
				} else if (reader.peek() == '}') {
					reader.skip();
					break;
				}
			}

			return new RecipeComponentBuilder(keys);
		}

		var key = reader.readUnquotedString();

		if (reader.canRead() && reader.peek() == ':') {
			reader.skip();
			key += ":" + reader.readUnquotedString();
		}

		RecipeComponent<?> component = simpleComponents.get(key);

		if (component == null) {
			var d = dynamicComponents.get(key);

			if (d != null) {
				component = d.readComponent(registries, this, reader);
			}
		}

		if (component == null) {
			throw new NullPointerException("Recipe Component '" + key + "' not found");
		}

		reader.skipWhitespace();

		while (reader.canRead() && reader.peek() == '[') {
			reader.skip();
			reader.skipWhitespace();
			boolean self = reader.canRead() && reader.peek() == '?';

			if (self) {
				reader.skip();
				reader.skipWhitespace();
			}

			reader.expect(']');
			component = self ? component.asListOrSelf() : component.asList();
		}

		return component;
	}
}
