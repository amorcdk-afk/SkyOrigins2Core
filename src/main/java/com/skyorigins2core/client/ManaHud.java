package com.skyorigins2core.client;

import com.skyorigins2core.SkyOrigins2Core;
import com.skyorigins2core.mana.ClientManaData;
import com.skyorigins2core.mana.ManaConfig;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The mana bar: a short blue bar in the XP bar's style, just right of the hotbar, labelled
// "Mana 72/100" above it in Minecraft's font. It moves further right when something else already
// sits there (the off-hand slot for left-handed players, or the attack indicator set to "Hotbar").
@Mod.EventBusSubscriber(modid = SkyOrigins2Core.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ManaHud
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "textures/gui/mana_bar.png");
    private static final int WIDTH = 64;
    private static final int HEIGHT = 5;
    // Gap between the hotbar's right edge and the bar
    private static final int GAP = 6;
    // Room taken by the off-hand slot or the hotbar attack indicator when they're on the right
    private static final int OFFHAND_WIDTH = 29;
    private static final int INDICATOR_WIDTH = 20;
    private static final int LABEL_COLOR = 0x6FC0FF;

    private ManaHud() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mana_bar", ManaHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        int max = ClientManaData.maxMana();
        int mana = Math.min(ClientManaData.mana(), max);
        if (!gui.shouldDrawSurvivalElements() || max <= 0)
            return;
        if (mana >= max && !ManaConfig.SHOW_BAR_WHEN_FULL.get())
            return;

        int x = screenWidth / 2 + 91 + GAP + rightSideOccupied();
        // Bar in the hotbar's lower half, label in its upper half
        int y = screenHeight - 8;
        graphics.blit(TEXTURE, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT * 2);
        int filled = (int) ((long) mana * (WIDTH + 1) / max);
        if (filled > 0)
            graphics.blit(TEXTURE, x, y, 0, HEIGHT, Math.min(filled, WIDTH), HEIGHT, WIDTH, HEIGHT * 2);

        Font font = gui.getFont();
        graphics.drawString(font, "Mana " + mana + "/" + max, x, y - 11, LABEL_COLOR, true);
    }

    // How much of the space right of the hotbar vanilla already uses
    private static int rightSideOccupied()
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null)
            return 0;
        if (player.getMainArm() == HumanoidArm.LEFT)
            return player.getOffhandItem().isEmpty() ? 0 : OFFHAND_WIDTH;
        return minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? INDICATOR_WIDTH : 0;
    }
}
