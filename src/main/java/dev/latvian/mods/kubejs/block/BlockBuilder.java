package dev.latvian.mods.kubejs.block;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.block.callbacks.AfterEntityFallenOnBlockCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.BlockExplodedCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.BlockStateMirrorCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.BlockStateModifyCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.BlockStateModifyPlacementCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.BlockStateRotateCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.CanBeReplacedCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.EntityFallenOnBlockCallbackJS;
import dev.latvian.mods.kubejs.block.callbacks.EntitySteppedOnBlockCallbackJS;
import dev.latvian.mods.kubejs.block.drop.BlockDropSupplier;
import dev.latvian.mods.kubejs.block.drop.BlockDrops;
import dev.latvian.mods.kubejs.block.entity.BlockEntityBuilder;
import dev.latvian.mods.kubejs.block.entity.BlockEntityInfo;
import dev.latvian.mods.kubejs.client.ModelGenerator;
import dev.latvian.mods.kubejs.client.VariantBlockStateGenerator;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ReturnsSelf
public abstract class BlockBuilder extends BuilderBase<Block> {
	private static final BlockBehaviour.StatePredicate ALWAYS_FALSE_STATE_PREDICATE = (blockState, blockGetter, blockPos) -> false;
	private static final BlockBehaviour.StateArgumentPredicate<?> ALWAYS_FALSE_STATE_ARG_PREDICATE = (blockState, blockGetter, blockPos, type) -> false;

	public transient SoundType soundType;
	public transient Function<BlockState, MapColor> mapColorFn;
	public transient float hardness;
	public transient float resistance;
	public transient float lightLevel;
	public transient boolean opaque;
	public transient boolean fullBlock;
	public transient boolean requiresTool;
	public transient BlockRenderType renderType;
	public transient BlockTintFunction tint;
	public transient final JsonObject textures;
	public transient String model;
	public transient ItemBuilder itemBuilder;
	public transient List<AABB> customShape;
	public transient boolean noCollision;
	public transient boolean notSolid;
	public transient float slipperiness = Float.NaN;
	public transient float speedFactor = Float.NaN;
	public transient float jumpFactor = Float.NaN;
	public Consumer<RandomTickCallbackJS> randomTickCallback;
	public BlockDropSupplier drops;
	public JsonObject blockstateJson;
	public JsonObject modelJson;
	public transient boolean noValidSpawns;
	public transient boolean suffocating;
	public transient boolean viewBlocking;
	public transient boolean redstoneConductor;
	public transient boolean transparent;
	public transient NoteBlockInstrument instrument;
	public transient Set<Property<?>> blockStateProperties;
	public transient Consumer<BlockStateModifyCallbackJS> defaultStateModification;
	public transient Consumer<BlockStateModifyPlacementCallbackJS> placementStateModification;
	public transient Predicate<CanBeReplacedCallbackJS> canBeReplacedFunction;
	public transient Consumer<EntitySteppedOnBlockCallbackJS> stepOnCallback;
	public transient Consumer<EntityFallenOnBlockCallbackJS> fallOnCallback;
	public transient Consumer<AfterEntityFallenOnBlockCallbackJS> afterFallenOnCallback;
	public transient Consumer<BlockExplodedCallbackJS> explodedCallback;
	public transient Consumer<BlockStateRotateCallbackJS> rotateStateModification;
	public transient Consumer<BlockStateMirrorCallbackJS> mirrorStateModification;
	public transient Consumer<BlockRightClickedKubeEvent> rightClick;
	public transient BlockEntityInfo blockEntityInfo;

	public BlockBuilder(ResourceLocation i) {
		super(i);
		soundType = SoundType.WOOD;
		mapColorFn = MapColorHelper.NONE;
		hardness = 1.5F;
		resistance = 3F;
		lightLevel = 0F;
		opaque = true;
		fullBlock = false;
		requiresTool = false;
		renderType = BlockRenderType.SOLID;
		textures = new JsonObject();
		textureAll(id.getNamespace() + ":block/" + id.getPath());
		model = "";
		itemBuilder = getOrCreateItemBuilder();

		if (itemBuilder instanceof BlockItemBuilder b) {
			b.blockBuilder = this;
		}

		customShape = new ArrayList<>();
		noCollision = false;
		notSolid = false;
		randomTickCallback = null;
		drops = null;
		blockstateJson = null;
		modelJson = null;
		noValidSpawns = false;
		suffocating = true;
		viewBlocking = true;
		redstoneConductor = true;
		transparent = false;
		blockStateProperties = new HashSet<>();
		defaultStateModification = null;
		placementStateModification = null;
		canBeReplacedFunction = null;
	}

	@Override
	public Block transformObject(Block obj) {
		obj.kjs$setBlockBuilder(this);
		return obj;
	}

	@Override
	public void createAdditionalObjects(AdditionalObjectRegistry registry) {
		if (itemBuilder != null) {
			registry.add(Registries.ITEM, itemBuilder);
		}

		if (blockEntityInfo != null) {
			registry.add(Registries.BLOCK_ENTITY_TYPE, new BlockEntityBuilder(id, blockEntityInfo));
		}
	}

	@Override
	@Info("""
		Sets the display name for this object, e.g. `Stone`.

		This will be overridden by a lang file if it exists.
		""")
	public BuilderBase<Block> displayName(Component name) {
		if (itemBuilder != null) {
			itemBuilder.displayName(name);
		}

		return super.displayName(name);
	}

	@Override
	public void generateDataJsons(KubeDataGenerator generator) {
		var table = generateLootTable();

		if (table != null) {
			generator.json(newID("loot_tables/blocks/", ""), LootTable.CODEC.encodeStart(JsonOps.INSTANCE, new Holder.Direct<>(table)).getOrThrow());
		}
	}

	@Nullable
	public LootTable generateLootTable() {
		if (drops == BlockDropSupplier.NO_DROPS) {
			return null;
		}

		var blockDrops = drops == null ? BlockDrops.createDefault(get().asItem().getDefaultInstance()) : drops.get();

		if (blockDrops.items().length == 0) {
			return null;
		}

		var pool = new LootPool.Builder();

		if (blockDrops.rolls() != null) {
			pool.setRolls(blockDrops.rolls());
		}

		pool.when(ExplosionCondition.survivesExplosion());

		for (var drop : blockDrops.items()) {
			var item = LootItem.lootTableItem(drop.getItem());

			if (drop.getCount() > 1) {
				item.apply(SetItemCountFunction.setCount(ConstantValue.exactly(drop.getCount())));
			}

			if (!drop.isComponentsPatchEmpty()) {
				item.apply(LootItemConditionalFunction.simpleBuilder(c -> new SetComponentsFunction(c, drop.getComponentsPatch())));
			}

			pool.add(item);
		}

		return new LootTable.Builder().withPool(pool).build();
	}

	@Override
	public void generateAssetJsons(KubeAssetGenerator generator) {
		if (blockstateJson != null) {
			generator.json(newID("blockstates/", ""), blockstateJson);
		} else {
			generator.blockState(id, this::generateBlockStateJson);
		}

		if (modelJson != null) {
			generator.json(newID("models/block/", ""), modelJson);
		} else {
			// This is different because there can be multiple models, so we should let the block handle those
			generateBlockModelJsons(generator);
		}

		if (itemBuilder != null) {
			if (itemBuilder.modelJson != null) {
				generator.json(newID("models/item/", ""), itemBuilder.modelJson);
			} else {
				generator.itemModel(itemBuilder.id, this::generateItemModelJson);
			}
		}

	}

	protected void generateItemModelJson(ModelGenerator m) {
		if (!model.isEmpty()) {
			m.parent(model);
		} else {
			m.parent(newID("block/", "").toString());
		}
	}

	protected void generateBlockModelJsons(KubeAssetGenerator generator) {
		generator.blockModel(id, mg -> {
			var particle = textures.get("particle").getAsString();

			if (areAllTexturesEqual(textures, particle)) {
				mg.parent("minecraft:block/cube_all");
				mg.texture("all", particle);
			} else {
				mg.parent("block/cube");
				mg.textures(textures);
			}

			if (tint != null || !customShape.isEmpty()) {
				List<AABB> boxes = new ArrayList<>(customShape);

				if (boxes.isEmpty()) {
					boxes.add(new AABB(0D, 0D, 0D, 1D, 1D, 1D));
				}

				for (var box : boxes) {
					mg.element(e -> {
						e.box(box);

						for (var direction : Direction.values()) {
							e.face(direction, face -> {
								face.tex("#" + direction.getSerializedName());
								face.cull();

								if (tint != null) {
									face.tintindex(0);
								}
							});
						}
					});
				}
			}
		});
	}

	protected void generateBlockStateJson(VariantBlockStateGenerator bs) {
		bs.simpleVariant("", model.isEmpty() ? (id.getNamespace() + ":block/" + id.getPath()) : model);
	}

	protected boolean areAllTexturesEqual(JsonObject tex, String t) {
		for (var direction : Direction.values()) {
			if (!tex.get(direction.getSerializedName()).getAsString().equals(t)) {
				return false;
			}
		}

		return true;
	}

	public BlockBuilder material(String material) {
		ConsoleJS.STARTUP.warn("blockBuilder.material(string) is no longer supported! Use .soundType(SoundType) and .mapColor(MapColor) instead!");
		return this;
	}

	@Info("Sets the block's sound type. Defaults to wood.")
	public BlockBuilder soundType(SoundType m) {
		if (m == null || m == SoundType.EMPTY) {
			soundType = SoundType.EMPTY;
			ConsoleJS.STARTUP.error("Invalid sound type!");
			ConsoleJS.STARTUP.warn("Valid sound types: " + SoundTypeWrapper.INSTANCE.getMap().keySet());
			return this;
		}

		soundType = m;
		return this;
	}

	public BlockBuilder noSoundType() {
		soundType = SoundType.EMPTY;
		return this;
	}

	public BlockBuilder woodSoundType() {
		return soundType(SoundType.WOOD);
	}

	public BlockBuilder stoneSoundType() {
		return soundType(SoundType.STONE);
	}

	public BlockBuilder gravelSoundType() {
		return soundType(SoundType.GRAVEL);
	}

	public BlockBuilder grassSoundType() {
		return soundType(SoundType.GRASS);
	}

	public BlockBuilder sandSoundType() {
		return soundType(SoundType.SAND);
	}

	public BlockBuilder cropSoundType() {
		return soundType(SoundType.CROP);
	}

	public BlockBuilder glassSoundType() {
		return soundType(SoundType.GLASS);
	}

	@Info("Sets the block's map color. Defaults to NONE.")
	public BlockBuilder mapColor(MapColor m) {
		mapColorFn = MapColorHelper.reverse(m);
		return this;
	}

	@Info("Sets the block's map color dynamically per block state. If unset, defaults to NONE.")
	public BlockBuilder dynamicMapColor(@Nullable Function<BlockState, Object> m) {
		mapColorFn = m == null ? MapColorHelper.NONE : s -> MapColorHelper.of(m.apply(s));
		return this;
	}

	@Info("""
		Sets the hardness of the block. Defaults to 1.5.
					
		Setting this to -1 will make the block unbreakable like bedrock.
		""")
	public BlockBuilder hardness(float h) {
		hardness = h;
		return this;
	}

	@Info("""
		Sets the blast resistance of the block. Defaults to 3.
		""")
	public BlockBuilder resistance(float r) {
		resistance = r;
		return this;
	}

	@Info("Makes the block unbreakable.")
	public BlockBuilder unbreakable() {
		hardness = -1F;
		resistance = Float.MAX_VALUE;
		return this;
	}

	@Info("Sets the light level of the block. Defaults to 0 (no light).")
	public BlockBuilder lightLevel(float light) {
		lightLevel = light;
		return this;
	}

	@Info("Sets the opacity of the block. Opaque blocks do not let light through.")
	public BlockBuilder opaque(boolean o) {
		opaque = o;
		return this;
	}

	@Info("Sets the block should be a full block or not, like cactus or doors.")
	public BlockBuilder fullBlock(boolean f) {
		fullBlock = f;
		return this;
	}

	@Info("Makes the block require a tool to have drops when broken.")
	public BlockBuilder requiresTool(boolean f) {
		requiresTool = f;
		return this;
	}

	@Info("Makes the block require a tool to have drops when broken.")
	public BlockBuilder requiresTool() {
		return requiresTool(true);
	}

	@Info("""
		Sets the render type of the block. Can be `cutout`, `cutout_mipped`, `translucent`, or `basic`.
		""")
	public BlockBuilder renderType(BlockRenderType l) {
		renderType = l;
		return this;
	}

	@Info("""
		Set the color of a specific layer of the block.
		""")
	public BlockBuilder color(int index, BlockTintFunction color) {
		if (!(tint instanceof BlockTintFunction.Mapped)) {
			tint = new BlockTintFunction.Mapped();
		}

		((BlockTintFunction.Mapped) tint).map.put(index, color);
		return this;
	}

	@Info("""
		Set the color of a specific layer of the block.
		""")
	public BlockBuilder color(BlockTintFunction color) {
		tint = color;
		return this;
	}

	@Info("""
		Texture the block on all sides with the same texture.
		""")
	public BlockBuilder textureAll(String tex) {
		for (var direction : Direction.values()) {
			textureSide(direction, tex);
		}

		textures.addProperty("particle", tex);
		return this;
	}

	@Info("""
		Texture a specific side of the block.
		""")
	public BlockBuilder textureSide(Direction direction, String tex) {
		return texture(direction.getSerializedName(), tex);
	}

	@Info("""
		Texture a specific texture key of the block.
		""")
	public BlockBuilder texture(String id, String tex) {
		textures.addProperty(id, tex);
		return this;
	}

	@Info("""
		Set the block's model.
		""")
	public BlockBuilder model(String m) {
		model = m;
		if (itemBuilder != null) {
			itemBuilder.parentModel(m);
		}
		return this;
	}

	@Info("""
		Modifies the block's item representation.
		""")
	public BlockBuilder item(@Nullable Consumer<ItemBuilder> i) {
		if (i == null) {
			itemBuilder = null;
			drops = BlockDropSupplier.NO_DROPS;
		} else {
			if (itemBuilder == null) {
				itemBuilder = getOrCreateItemBuilder();

				if (itemBuilder instanceof BlockItemBuilder b) {
					b.blockBuilder = this;
				}

				ScriptType.STARTUP.console.warn("`item` is called with non-null builder callback after block item is set to null! Creating another block item as fallback.");
			}
			i.accept(itemBuilder);
		}

		return this;
	}

	@HideFromJS
	protected ItemBuilder getOrCreateItemBuilder() {
		return itemBuilder == null ? (itemBuilder = new BlockItemBuilder(id)) : itemBuilder;
	}

	@Info("""
		Set the block to have no corresponding item.
		""")
	public BlockBuilder noItem() {
		return item(null);
	}

	@Info("Set the shape of the block.")
	public BlockBuilder box(double x0, double y0, double z0, double x1, double y1, double z1, boolean scale16) {
		if (scale16) {
			customShape.add(new AABB(x0 / 16D, y0 / 16D, z0 / 16D, x1 / 16D, y1 / 16D, z1 / 16D));
		} else {
			customShape.add(new AABB(x0, y0, z0, x1, y1, z1));
		}

		return this;
	}

	@Info("Set the shape of the block.")
	public BlockBuilder box(double x0, double y0, double z0, double x1, double y1, double z1) {
		return box(x0, y0, z0, x1, y1, z1, true);
	}

	public static VoxelShape createShape(List<AABB> boxes) {
		if (boxes.isEmpty()) {
			return Shapes.block();
		}

		var shape = Shapes.create(boxes.getFirst());

		for (var i = 1; i < boxes.size(); i++) {
			shape = Shapes.or(shape, Shapes.create(boxes.get(i)));
		}

		return shape;
	}

	@Info("Makes the block not collide with entities.")
	public BlockBuilder noCollision() {
		noCollision = true;
		return this;
	}

	@Info("Makes the block not be solid.")
	public BlockBuilder notSolid() {
		notSolid = true;
		return this;
	}

	@Deprecated(forRemoval = true)
	public BlockBuilder setWaterlogged(boolean waterlogged) {
		ScriptType.STARTUP.console.warn("\"BlockBuilder.waterlogged\" is a deprecated property! Please use \"BlockBuilder.property(BlockProperties.WATERLOGGED)\" instead.");
		if (waterlogged) {
			property(BlockStateProperties.WATERLOGGED);
		}
		return this;
	}

	@Deprecated(forRemoval = true)
	public boolean getWaterlogged() {
		ScriptType.STARTUP.console.warn("\"BlockBuilder.waterlogged\" is a deprecated property! Please use \"BlockBuilder.property(BlockProperties.WATERLOGGED)\" instead.");
		return canBeWaterlogged();
	}

	@Info("Makes the block can be waterlogged.")
	public BlockBuilder waterlogged() {
		return property(BlockStateProperties.WATERLOGGED);
	}

	@Info("Checks if the block can be waterlogged.")
	public boolean canBeWaterlogged() {
		return blockStateProperties.contains(BlockStateProperties.WATERLOGGED);
	}

	@Info("Change drops of this block")
	public BlockBuilder drops(BlockDropSupplier drops) {
		this.drops = drops == null ? BlockDropSupplier.NO_DROPS : drops;
		return this;
	}

	@Info("Clears all drops for the block.")
	public BlockBuilder noDrops() {
		drops = BlockDropSupplier.NO_DROPS;
		return this;
	}

	@Info("Set how slippery the block is.")
	public BlockBuilder slipperiness(float f) {
		slipperiness = f;
		return this;
	}

	@Info("""
		Set how fast you can walk on the block.
					
		Any value above 1 will make you walk insanely fast as your speed is multiplied by this value each tick.
					
		Recommended values are between 0.1 and 1, useful for mimicking soul sand or ice.
		""")
	public BlockBuilder speedFactor(float f) {
		speedFactor = f;
		return this;
	}

	@Info("Set how high you can jump on the block.")
	public BlockBuilder jumpFactor(float f) {
		jumpFactor = f;
		return this;
	}

	/**
	 * Sets random tick callback for this black.
	 *
	 * @param randomTickCallback A callback using a block container and a random.
	 */
	@Info("Sets random tick callback for this black.")
	public BlockBuilder randomTick(@Nullable Consumer<RandomTickCallbackJS> randomTickCallback) {
		this.randomTickCallback = randomTickCallback;
		return this;
	}

	@Info("Makes mobs not spawn on the block.")
	public BlockBuilder noValidSpawns(boolean b) {
		noValidSpawns = b;
		return this;
	}

	@Info("Makes the block suffocating.")
	public BlockBuilder suffocating(boolean b) {
		suffocating = b;
		return this;
	}

	@Info("Makes the block view blocking.")
	public BlockBuilder viewBlocking(boolean b) {
		viewBlocking = b;
		return this;
	}

	@Info("Makes the block a redstone conductor.")
	public BlockBuilder redstoneConductor(boolean b) {
		redstoneConductor = b;
		return this;
	}

	@Info("Makes the block transparent.")
	public BlockBuilder transparent(boolean b) {
		transparent = b;
		return this;
	}

	@Info("Helper method for setting the render type of the block to `cutout` correctly.")
	public BlockBuilder defaultCutout() {
		return renderType(BlockRenderType.CUTOUT).notSolid().noValidSpawns(true).suffocating(false).viewBlocking(false).redstoneConductor(false).transparent(true);
	}

	@Info("Helper method for setting the render type of the block to `translucent` correctly.")
	public BlockBuilder defaultTranslucent() {
		return defaultCutout().renderType(BlockRenderType.TRANSLUCENT);
	}

	@Info("Note block instrument.")
	public BlockBuilder instrument(NoteBlockInstrument i) {
		instrument = i;
		return this;
	}

	@Override
	@Info("Tags both the block and the item with the given tag.")
	public BlockBuilder tag(ResourceLocation[] tag) {
		return tagBoth(tag);
	}

	@Info("Tags both the block and the item with the given tag.")
	public BlockBuilder tagBoth(ResourceLocation[] tag) {
		tagBlock(tag);
		tagItem(tag);
		return this;
	}

	@Info("Tags the block with the given tag.")
	public BlockBuilder tagBlock(ResourceLocation[] tag) {
		super.tag(tag);
		return this;
	}

	@Info("Tags the item with the given tag.")
	public BlockBuilder tagItem(ResourceLocation[] tag) {
		itemBuilder.tag(tag);
		return this;
	}

	@Info("Set the default state of the block.")
	public BlockBuilder defaultState(Consumer<BlockStateModifyCallbackJS> callbackJS) {
		defaultStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback for determining the blocks state when placed.")
	public BlockBuilder placementState(Consumer<BlockStateModifyPlacementCallbackJS> callbackJS) {
		placementStateModification = callbackJS;
		return this;
	}

	@Info("Set if the block can be replaced by something else.")
	public BlockBuilder canBeReplaced(Predicate<CanBeReplacedCallbackJS> callbackJS) {
		canBeReplacedFunction = callbackJS;
		return this;
	}

	@Info("""
		Set what happens when an entity steps on the block
		This is called every tick for every entity standing on the block, so be careful what you do here.
		""")
	public BlockBuilder steppedOn(Consumer<EntitySteppedOnBlockCallbackJS> callbackJS) {
		stepOnCallback = callbackJS;
		return this;
	}

	@Info("Set what happens when an entity falls on the block. Do not use this for moving them, use bounce instead!")
	public BlockBuilder fallenOn(Consumer<EntityFallenOnBlockCallbackJS> callbackJS) {
		fallOnCallback = callbackJS;
		return this;
	}

	@Info("""
		Bounces entities that land on this block by bounciness * their fall velocity.
		Do not make bounciness negative, as that is a recipe for a long and laggy trip to the void
		""")
	public BlockBuilder bounciness(float bounciness) {
		return afterFallenOn(ctx -> ctx.bounce(bounciness));
	}

	@Info("""
		Set how this block bounces/moves entities that land on top of this. Do not use this to modify the block, use fallOn instead!
		Use ctx.bounce(height) or ctx.setVelocity(x, y, z) to change the entities velocity.
		""")
	public BlockBuilder afterFallenOn(Consumer<AfterEntityFallenOnBlockCallbackJS> callbackJS) {
		afterFallenOnCallback = callbackJS;
		return this;
	}

	@Info("Set how this block reacts after an explosion. Note the block has already been destroyed at this point")
	public BlockBuilder exploded(Consumer<BlockExplodedCallbackJS> callbackJS) {
		explodedCallback = callbackJS;
		return this;
	}

	@Info("""
		Add a blockstate property to the block.
					
		For example, facing, lit, etc.
		""")
	public BlockBuilder property(Property<?> property) {
		if (property.getPossibleValues().size() <= 1) {
			throw new IllegalArgumentException(String.format("Block \"%s\" has an illegal Blockstate Property \"%s\" which has <= 1 possible values. (%d possible values)", id, property.getName(), property.getPossibleValues().size()));
		}
		blockStateProperties.add(property);
		return this;
	}

	@Info("Set the callback used for determining how the block rotates")
	public BlockBuilder rotateState(Consumer<BlockStateRotateCallbackJS> callbackJS) {
		rotateStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback used for determining how the block is mirrored")
	public BlockBuilder mirrorState(Consumer<BlockStateMirrorCallbackJS> callbackJS) {
		mirrorStateModification = callbackJS;
		return this;
	}

	@Info("Set the callback used for right-clicking on the block")
	public BlockBuilder rightClick(Consumer<BlockRightClickedKubeEvent> callbackJS) {
		rightClick = callbackJS;
		return this;
	}

	@Info("Creates a Block Entity for this block")
	public BlockBuilder blockEntity(Consumer<BlockEntityInfo> callback) {
		blockEntityInfo = new BlockEntityInfo(this);
		callback.accept(blockEntityInfo);
		return this;
	}

	public Block.Properties createProperties() {
		// FIXME: Implement all the other ex-Material properties
		//var properties = BlockBehaviour.Properties.of();
		var properties = new KubeJSBlockProperties(this);
		properties.sound(soundType);
		properties.mapColor(mapColorFn);

		if (resistance >= 0F) {
			properties.strength(hardness, resistance);
		} else {
			properties.strength(hardness);
		}

		properties.lightLevel(state -> (int) (lightLevel * 15F));

		if (noCollision) {
			properties.noCollission();
		}

		if (notSolid) {
			properties.noOcclusion();
		}

		if (requiresTool) {
			properties.requiresCorrectToolForDrops();
		}

		if (drops == BlockDropSupplier.NO_DROPS) {
			properties.noLootTable();
		}

		if (!Float.isNaN(slipperiness)) {
			properties.friction(slipperiness);
		}

		if (!Float.isNaN(speedFactor)) {
			properties.speedFactor(speedFactor);
		}

		if (!Float.isNaN(jumpFactor)) {
			properties.jumpFactor(jumpFactor);
		}

		if (noValidSpawns) {
			properties.isValidSpawn(Cast.to(ALWAYS_FALSE_STATE_ARG_PREDICATE));
		}

		if (!suffocating) {
			properties.isSuffocating(ALWAYS_FALSE_STATE_PREDICATE);
		}

		if (!viewBlocking) {
			properties.isViewBlocking(ALWAYS_FALSE_STATE_PREDICATE);
		}

		if (!redstoneConductor) {
			properties.isRedstoneConductor(ALWAYS_FALSE_STATE_PREDICATE);
		}

		if (randomTickCallback != null) {
			properties.randomTicks();
		}

		if (instrument != null) {
			properties.instrument(instrument);
		}

		return properties;
	}
}