package dev.latvian.mods.kubejs.fluid;

import dev.latvian.mods.kubejs.block.BlockRenderType;
import dev.latvian.mods.kubejs.color.Color;
import dev.latvian.mods.kubejs.color.SimpleColor;
import dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

@ReturnsSelf
public class FluidBuilder extends BuilderBase<FlowingFluid> {
	public static final Color WATER_COLOR = new SimpleColor(0xFF3F76E4);

	public transient Color bucketTint = null;

	public transient int slopeFindDistance = 4;
	public transient int levelDecreasePerBlock = 1;
	public transient float explosionResistance = 1;
	public transient int tickRate = 5;

	public FluidTypeBuilder fluidType;
	public FlowingFluidBuilder flowingFluid;
	public FluidBlockBuilder block;
	public FluidBucketItemBuilder bucketItem;
	private BaseFlowingFluid.Properties properties;

	public FluidBuilder(ResourceLocation i) {
		super(i);
		fluidType = new FluidTypeBuilder(id);
		flowingFluid = new FlowingFluidBuilder(this);
		block = new FluidBlockBuilder(this);
		bucketItem = new FluidBucketItemBuilder(this);
	}

	@Override
	public BuilderBase<FlowingFluid> displayName(Component name) {
		if (block != null) {
			block.displayName(name);
		}

		if (bucketItem != null) {
			bucketItem.displayName(Component.literal("").append(name).append(" Bucket"));
		}

		return super.displayName(name);
	}

	public BaseFlowingFluid.Properties createProperties() {
		if (properties == null) {
			properties = new BaseFlowingFluid.Properties(fluidType, this, flowingFluid);
			properties.bucket(bucketItem);
			properties.block((Supplier) block);
			properties.slopeFindDistance(slopeFindDistance);
			properties.levelDecreasePerBlock(levelDecreasePerBlock);
			properties.explosionResistance(explosionResistance);
			properties.tickRate(tickRate);
		}

		return properties;
	}

	@Override
	public FlowingFluid createObject() {
		return new BaseFlowingFluid.Source(createProperties());
	}

	@Override
	public void createAdditionalObjects(AdditionalObjectRegistry registry) {
		registry.add(NeoForgeRegistries.Keys.FLUID_TYPES, fluidType);
		registry.add(Registries.FLUID, flowingFluid);

		if (block != null) {
			registry.add(Registries.BLOCK, block);
		}

		if (bucketItem != null) {
			registry.add(Registries.ITEM, bucketItem);
		}
	}

	@Override
	public BuilderBase<FlowingFluid> tag(ResourceLocation[] tag) {
		this.flowingFluid.tag(tag);
		return super.tag(tag);
	}

	public FluidBuilder type(Consumer<FluidTypeBuilder> builder) {
		builder.accept(fluidType);
		return this;
	}

	public FluidBuilder tint(Color c) {
		fluidType.tint = c;
		return this;
	}

	public FluidBuilder bucketTint(Color c) {
		this.bucketTint = c;
		return this;
	}

	public FluidBuilder stillTexture(ResourceLocation id) {
		fluidType.stillTexture = id;
		return this;
	}

	public FluidBuilder flowingTexture(ResourceLocation id) {
		fluidType.flowingTexture = id;
		return this;
	}

	public FluidBuilder renderType(BlockRenderType l) {
		fluidType.renderType = l;
		return this;
	}

	public FluidBuilder translucent() {
		return renderType(BlockRenderType.TRANSLUCENT);
	}

	public FluidBuilder slopeFindDistance(int slopeFindDistance) {
		this.slopeFindDistance = slopeFindDistance;
		return this;
	}

	public FluidBuilder levelDecreasePerBlock(int levelDecreasePerBlock) {
		this.levelDecreasePerBlock = levelDecreasePerBlock;
		return this;
	}

	public FluidBuilder explosionResistance(float explosionResistance) {
		this.explosionResistance = explosionResistance;
		return this;
	}

	public FluidBuilder tickRate(int tickRate) {
		this.tickRate = tickRate;
		return this;
	}

	public FluidBuilder noBucket() {
		this.bucketItem = null;
		return this;
	}

	public FluidBuilder noBlock() {
		this.block = null;
		return this;
	}
}