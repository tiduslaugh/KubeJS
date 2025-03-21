package dev.latvian.mods.kubejs.fluid;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockRenderType;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FluidBlockBuilder extends BlockBuilder {
	public final FluidBuilder fluidBuilder;

	public FluidBlockBuilder(FluidBuilder b) {
		super(b.id);
		fluidBuilder = b;
		defaultTranslucent();
		noItem();
		noDrops();
		renderType(BlockRenderType.SOLID);
	}

	@Override
	public Block createObject() {
		return new LiquidBlock(fluidBuilder.get(), Block.Properties.ofFullCopy(Blocks.WATER).noCollission().strength(100F).noLootTable());
	}

	@Override
	public void generateAssetJsons(KubeAssetGenerator generator) {
		generator.blockState(id, m -> m.simpleVariant("", id.getNamespace() + ":block/" + id.getPath()));
		generator.blockModel(id, m -> {
			m.parent("");
			m.texture("particle", fluidBuilder.fluidType.stillTexture.toString());
		});
	}

	@Override
	public BlockBuilder item(@Nullable Consumer<ItemBuilder> i) {
		if (i != null) {
			throw new IllegalStateException("Fluid blocks cannot have items!");
		} else {
			return super.item(null);
		}
	}
}
