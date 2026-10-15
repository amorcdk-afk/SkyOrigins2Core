package com.skyorigins2core.client;

import com.skyorigins2core.SkyOrigins2Core;
import com.skyorigins2core.mana.ClientManaData;
import com.skyorigins2core.mana.ManaConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The mana bar: a blue copy of the XP bar's look, just above the XP bar, with the mana number in
// Minecraft's font above its middle (like the XP level). It's drawn before hearts and hunger and
// raises them a row, the way vanilla stacks its status bars.
@Mod.EventBusSubscriber(modid = SkyOrigins2Core.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ManaHud
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "textures/gui/mana_bar.png");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    // Bar top sits 12px above the XP bar's top, leaving room for the XP level number between them
    private static final int OFFSET_FROM_BOTTOM = 41;
    // How far hearts, armor, hunger and air move up to make room
    private static final int RAISE = 12;
    private static final int TEXT_COLOR = 0x6FC0FF;

    private ManaHud() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        // Before the health overlay, so the raise below applies to hearts and everything stacked on them
        event.registerBelow(VanillaGuiOverlay.PLAYER_HEALTH.id(), "mana_bar", ManaHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        int max = ClientManaData.maxMana();
        int mana = Math.min(ClientManaData.mana(), max);
        if (!gui.shouldDrawSurvivalElements() || max <= 0)
            return;
        if (mana >= max && !ManaConfig.SHOW_BAR_WHEN_FULL.get())
            return;

        gui.leftHeight += RAISE;
        gui.rightHeight += RAISE;

        int x = screenWidth / 2 - 91;
        int y = screenHeight - OFFSET_FROM_BOTTOM;
        graphics.blit(TEXTURE, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT * 2);
        int filled = (int) ((long) mana * (WIDTH + 1) / max);
        if (filled > 0)
            graphics.blit(TEXTURE, x, y, 0, HEIGHT, Math.min(filled, WIDTH), HEIGHT, WIDTH, HEIGHT * 2);

        // The number, outlined in black like the XP level
        Font font = gui.getFont();
        String text = String.valueOf(mana);
        int textX = (screenWidth - font.width(text)) / 2;
        int textY = y - 6;
        graphics.drawString(font, text, textX + 1, textY, 0, false);
        graphics.drawString(font, text, textX - 1, textY, 0, false);
        graphics.drawString(font, text, textX, textY + 1, 0, false);
        graphics.drawString(font, text, textX, textY - 1, 0, false);
        graphics.drawString(font, text, textX, textY, TEXT_COLOR, false);
    }
}
