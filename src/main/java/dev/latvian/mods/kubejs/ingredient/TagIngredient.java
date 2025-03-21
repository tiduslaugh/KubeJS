package dev.latvian.mods.kubejs.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.error.EmptyTagException;
import dev.latvian.mods.kubejs.recipe.CachedTagLookup;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class TagIngredient implements KubeJSIngredient {
	public static final MapCodec<TagIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(t -> t.tagKey)
	).apply(instance, tagKey -> new TagIngredient(null, tagKey)));

	public static final StreamCodec<ByteBuf, TagIngredient> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(id -> new TagIngredient(null, ItemTags.create(id)), in -> in.tagKey.location());

	public final @Nullable CachedTagLookup<Item> lookup;
	public final TagKey<Item> tagKey;
	private Set<Item> cachedItems;

	public TagIngredient(@Nullable CachedTagLookup<Item> lookup, TagKey<Item> tagKey) {
		this.lookup = lookup;
		this.tagKey = tagKey;
	}

	@Override
	public IngredientType<?> getType() {
		return KubeJSIngredients.TAG.get();
	}

	public Set<Item> kjs$getItems() {
		if (cachedItems == null) {
			if (lookup != null) {
				cachedItems = lookup.values(tagKey);
			} else {
				cachedItems = new HashSet<>();

				for (var item : BuiltInRegistries.ITEM) {
					if (item.builtInRegistryHolder().is(tagKey)) {
						cachedItems.add(item);
					}
				}
			}

			cachedItems = Set.copyOf(cachedItems);
		}

		return cachedItems;
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		if (lookup != null) {
			return stack != null && kjs$getItems().contains(stack.getItem());
		} else {
			return stack != null && stack.is(tagKey);
		}
	}

	@Override
	public Stream<ItemStack> getItems() {
		var set = kjs$getItems();

		if (set.isEmpty()) {
			if (RecipesKubeEvent.TEMP_ITEM_TAG_LOOKUP.getValue() != null) {
				throw new EmptyTagException(tagKey);
			} else {
				var error = new ItemStack(Items.BARRIER);
				error.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tagKey.location()));
				return Stream.of(error);
			}
		}

		return set.stream().map(ItemStack::new);
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof TagIngredient i && tagKey == i.tagKey;
	}

	@Override
	public int hashCode() {
		return tagKey.hashCode();
	}

	@Override
	public String toString() {
		return "KubeJSItemTagIngredient[" + tagKey.location() + "]";
	}
}
