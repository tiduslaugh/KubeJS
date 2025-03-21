package dev.latvian.mods.kubejs.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.bindings.event.ClientEvents;
import dev.latvian.mods.kubejs.bindings.event.ItemEvents;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.client.BlockEntityRendererRegistryKubeEvent;
import dev.latvian.mods.kubejs.client.BlockTintFunctionWrapper;
import dev.latvian.mods.kubejs.client.EntityRendererRegistryKubeEvent;
import dev.latvian.mods.kubejs.client.ItemTintFunctionWrapper;
import dev.latvian.mods.kubejs.client.KubeJSClient;
import dev.latvian.mods.kubejs.client.KubeJSResourcePackFinder;
import dev.latvian.mods.kubejs.client.MenuScreenRegistryKubeEvent;
import dev.latvian.mods.kubejs.fluid.FluidBlockBuilder;
import dev.latvian.mods.kubejs.fluid.FluidBucketItemBuilder;
import dev.latvian.mods.kubejs.fluid.FluidBuilder;
import dev.latvian.mods.kubejs.gui.KubeJSMenus;
import dev.latvian.mods.kubejs.gui.KubeJSScreen;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.item.ItemModelPropertiesKubeEvent;
import dev.latvian.mods.kubejs.kubedex.KubedexHighlight;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

@EventBusSubscriber(modid = KubeJS.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class KubeJSNeoForgeClient {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void setupClient(FMLClientSetupEvent event) {
		KubeJS.PROXY = new KubeJSClient();
		event.enqueueWork(KubeJSNeoForgeClient::setupClient0);
	}

	@SubscribeEvent
	public static void addClientPacks(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
			event.addRepositorySource(new KubeJSResourcePackFinder());
		}
	}

	private static void setupClient0() {
		ItemEvents.MODEL_PROPERTIES.post(ScriptType.STARTUP, new ItemModelPropertiesKubeEvent());

		for (var builder : RegistryObjectStorage.BLOCK) {
			if (builder instanceof BlockBuilder b) {
				switch (b instanceof FluidBlockBuilder fb ? fb.fluidBuilder.fluidType.renderType : b.renderType) {
					// TODO: Move these to model json
					case CUTOUT -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.cutout());
					case CUTOUT_MIPPED -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.cutoutMipped());
					case TRANSLUCENT -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.translucent());
				}
			}
		}

		for (var builder : RegistryObjectStorage.FLUID) {
			if (builder instanceof FluidBuilder b) {
				switch (b.fluidType.renderType) {
					case CUTOUT -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.cutout());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.cutout());
					}
					case CUTOUT_MIPPED -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.cutoutMipped());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.cutoutMipped());
					}
					case TRANSLUCENT -> {
						ItemBlockRenderTypes.setRenderLayer(b.get().getSource(), RenderType.translucent());
						ItemBlockRenderTypes.setRenderLayer(b.get().getFlowing(), RenderType.translucent());
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void blockColors(RegisterColorHandlersEvent.Block event) {
		for (var builder : RegistryObjectStorage.BLOCK) {
			if (builder instanceof BlockBuilder b && b.tint != null) {
				event.register(new BlockTintFunctionWrapper(b.tint), b.get());
			}
		}
	}

	@SubscribeEvent
	public static void itemColors(RegisterColorHandlersEvent.Item event) {
		for (var builder : RegistryObjectStorage.ITEM) {
			if (builder instanceof ItemBuilder b && b.tint != null) {
				event.register(new ItemTintFunctionWrapper(b.tint), b.get());
			}

			if (builder instanceof FluidBucketItemBuilder b && (b.fluidBuilder.bucketTint != null || b.fluidBuilder.fluidType.tint != null)) {
				event.register((stack, index) -> index == 1 ? (b.fluidBuilder.bucketTint == null ? b.fluidBuilder.fluidType.tint : b.fluidBuilder.bucketTint).getArgbJS() : 0xFFFFFFFF, b.get());
			}
		}
	}

	@SubscribeEvent
	public static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(KubeJSMenus.MENU.get(), KubeJSScreen::new);
		ClientEvents.MENU_SCREEN_REGISTRY.post(ScriptType.STARTUP, new MenuScreenRegistryKubeEvent(event));
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		ClientEvents.ENTITY_RENDERER_REGISTRY.post(ScriptType.STARTUP, new EntityRendererRegistryKubeEvent(event));
		ClientEvents.BLOCK_ENTITY_RENDERER_REGISTRY.post(ScriptType.STARTUP, new BlockEntityRendererRegistryKubeEvent(event));
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(KubedexHighlight.keyMapping = new KeyMapping("key.kubejs.kubedex", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.kubejs"));
	}

	@SubscribeEvent
	public static void registerCoreShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ID.mc("kubejs/rendertype_highlight"), DefaultVertexFormat.POSITION_COLOR), s -> KubedexHighlight.INSTANCE.highlightShader = s);
	}
}
