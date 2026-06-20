package com.kalypso.mod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// 1. ADIM: NeoForge 1.21.8'e mod kimliğini kesin olarak kaydetme
@Mod("kalypso")
public class KalypsoMod {

    // Global Durumlar
    public static boolean isInterfaceOpen = true; 
    public static int activeTab = 0;             
    public static String injectionStatus = "Not Injected"; 
    public static int injectionStateCode = 0;    
    private static long pendingStartTime = 0;

    public static final List<String> editorLines = new ArrayList<>();
    public static boolean silentPacketMode = false;
    public static boolean velocityFilter = false;
    public static boolean autoInject = false;
    public static boolean glassmorphismUltra = true;

    static {
        editorLines.add("-- Amethyst Custom Script");
        editorLines.add("function OverrideGrim()");
        editorLines.add("    local gate = AntiCheat.Detect()");
        editorLines.add("    if gate == \"GrimAC\" then");
        editorLines.add("        LocalPlayer.SendSilentPacket({ calculation = \"amethyst_lerp\", secure = true })");
        editorLines.add("    end");
        editorLines.add("end");
        editorLines.add("");
        editorLines.add("OverrideGrim()");
    }

    // 2. ADIM: NeoForge'un bu sınıfı ve olayları (eventleri) tanıması için zorunlu Constructor
    public KalypsoMod(IEventBus modEventBus) {
        // Çizim ve klavye olaylarını NeoForge ana motoruna kaydediyoruz
        NeoForge.EVENT_BUS.register(ClientEvents.class);
    }

    // 3. ADIM: Tüm çizim ve girdi motorunu ayrı bir Client-Side alt sınıfına taşıyoruz (Çökmeyi önler)
    @EventBusSubscriber(modid = "kalypso", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientEvents {

        // --- KLAVYE MOTORU ---
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            if (event.getKey() == GLFW.GLFW_KEY_F9 && event.getAction() == GLFW.GLFW_PRESS) {
                isInterfaceOpen = !isInterfaceOpen;
                if (isInterfaceOpen) {
                    mc.mouseHandler.releaseMouse();
                } else {
                    mc.mouseHandler.grabMouse();
                }
            }

            if (isInterfaceOpen && event.getAction() == GLFW.GLFW_PRESS) {
                if (event.getKey() == GLFW.GLFW_KEY_DOWN) {
                    activeTab = (activeTab + 1) % 4;
                } else if (event.getKey() == GLFW.GLFW_KEY_UP) {
                    activeTab = (activeTab - 1 + 4) % 4;
                }
            }
        }

        @SubscribeEvent
        public static void onMouseClickPre(ScreenEvent.MouseButtonPressed.Pre event) {
            if (isInterfaceOpen) {
                event.setCanceled(true); // Panel açıkken arkaya tıklanmasını engelle
            }
        }

        // --- RENDER MOTORU (GUI GRAPHICS) ---
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            if (injectionStateCode == 1) {
                if (System.currentTimeMillis() - pendingStartTime > 1200) {
                    injectionStateCode = 2;
                    injectionStatus = "KALYPSO Connected";
                    mc.player.displayClientMessage(Component.literal("§d[KALYPSO] §aAmethyst Core injected successfully!"), true);
                }
            }

            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            if (!isInterfaceOpen) {
                event.getGuiGraphics().drawString(mc.font, "INCOGNITO MODE - PRESS F9 TO OPEN", 15, 15, 0x1EFFFFFF, false);
                return;
            }

            // Siber Karartma ve Cam Efekti Dolguları
            event.getGuiGraphics().fill(0, 0, width, height, 0x77030206);
            event.getGuiGraphics().fill(0, 0, 120, 120, 0x15A855F7);
            event.getGuiGraphics().fill(width - 120, height - 120, width, height, 0x12C084FC);

            // Ana Kasa Çerçevesi
            int pad = 30;
            int fx1 = pad, fy1 = pad;
            int fx2 = width - pad, fy2 = height - pad;
            
            event.getGuiGraphics().fill(fx1, fy1, fx2, fy2, 0xEB06050A);
            event.getGuiGraphics().renderOutline(fx1, fy1, fx2 - fx1, fy2 - fy1, 0x1AD8B4FE);

            // SOL SIDEBAR
            int sidebarWidth = 160;
            int sbX2 = fx1 + sidebarWidth;
            event.getGuiGraphics().fill(sbX2, fy1, sbX2 + 1, fy2, 0x0AD8B4FE);
            event.getGuiGraphics().drawString(mc.font, "🔱 KALYPSO", fx1 + 15, fy1 + 25, 0xFFFFFFFF, true);

            int btnY = fy1 + 65;
            int btnHeight = 22;
            int btnPadding = 6;

            drawNavButton(event, "  Terminal  Executor", fx1 + 10, btnY, sidebarWidth - 20, btnHeight, activeTab == 0);
            btnY += btnHeight + btnPadding;
            drawNavButton(event, "  Shield  GrimAC Bypass", fx1 + 10, btnY, sidebarWidth - 20, btnHeight, activeTab == 1);
            btnY += btnHeight + btnPadding;
            drawNavButton(event, "  Archive  Script Hub", fx1 + 10, btnY, sidebarWidth - 20, btnHeight, activeTab == 2);
            btnY += btnHeight + btnPadding;
            drawNavButton(event, "  Gear  Settings", fx1 + 10, btnY, sidebarWidth - 20, btnHeight, activeTab == 3);

            // Discord Alanı
            int dcY1 = fy2 - 50;
            event.getGuiGraphics().fill(fx1 + 10, dcY1, sbX2 - 10, fy2 - 10, 0x0D5865F2);
            event.getGuiGraphics().renderOutline(fx1 + 10, dcY1, (sbX2 - 10) - (fx1 + 10), (fy2 - 10) - dcY1, 0x265865F2);
            event.getGuiGraphics().drawString(mc.font, "Kalypso Community", fx1 + 15, dcY1 + 8, 0xFFFFFFFF, false);
            event.getGuiGraphics().drawString(mc.font, "Join Discord server", fx1 + 15, dcY1 + 22, 0xFF9CA3AF, false);

            // SAĞ SEKMELİ WORKSPACE İÇERİKLERİ
            int workX1 = sbX2 + 20;
            int workY1 = fy1 + 20;
            int workX2 = fx2 - 20;
            int workY2 = fy2 - 35;

            if (activeTab == 0) {
                event.getGuiGraphics().fill(workX1, workY1, workX2, workY1 + 25, 0x33000000);
                event.getGuiGraphics().drawString(mc.font, "main.lua", workX1 + 15, workY1 + 8, 0xFF4B5563, false);
                event.getGuiGraphics().drawString(mc.font, "[Plug Attach]", workX2 - 200, workY1 + 8, (injectionStateCode == 2) ? 0xFF34D399 : 0xFF4B5563, false);
                event.getGuiGraphics().drawString(mc.font, "[Trash Clear]", workX2 - 120, workY1 + 8, 0xFF4B5563, false);
                event.getGuiGraphics().drawString(mc.font, "[Play Run]", workX2 - 50, workY1 + 8, 0xFFC084FC, false);

                int edY1 = workY1 + 26;
                event.getGuiGraphics().fill(workX1, edY1, workX2, workY2, 0x80000000);
                event.getGuiGraphics().renderOutline(workX1, edY1, workX2 - workX1, workY2 - edY1, 0x08C084FC);

                int lineRenderY = edY1 + 15;
                for (String line : editorLines) {
                    if (lineRenderY + 12 > workY2) break;
                    int tokenColor = 0xFFD8B4FE;
                    if (line.startsWith("--")) tokenColor = 0xFF6B7280;
                    else if (line.contains("function") || line.contains("if") || line.contains("end")) tokenColor = 0xFFF472B6;
                    
                    event.getGuiGraphics().drawString(mc.font, line, workX1 + 20, lineRenderY, tokenColor, false);
                    lineRenderY += 14;
                }
            } else if (activeTab == 1) {
                event.getGuiGraphics().drawString(mc.font, "GrimAC Anticheat Bypass", workX1, workY1, 0xFFFFFFFF, true);
                int cardY = workY1 + 40;
                drawConfigCard(event, "Silent Packet Mode", "Giden verileri sunucuya insan hizinda gonderir.", workX1, cardY, workX2 - workX1, silentPacketMode);
                cardY += 55;
                drawConfigCard(event, "Velocity Filter", "Geri tepme ve hiz verilerindeki degisiklikleri maskeler.", workX1, cardY, workX2 - workX1, velocityFilter);
            } else if (activeTab == 2) {
                event.getGuiGraphics().drawString(mc.font, "Script Hub", workX1, workY1, 0xFFFFFFFF, true);
                int cardY = workY1 + 40;
                drawConfigCard(event, "Universal Sim Optimizer", "Simulator oyunlari icin genel optimizasyon.", workX1, cardY, workX2 - workX1, false);
                cardY += 55;
                drawConfigCard(event, "Visual ESP Matrix", "Tum haritadaki varliklari izletir.", workX1, cardY, workX2 - workX1, true);
            } else if (activeTab == 3) {
                event.getGuiGraphics().drawString(mc.font, "System Settings", workX1, workY1, 0xFFFFFFFF, true);
                int cardY = workY1 + 40;
                drawConfigCard(event, "Auto-Inject", "Oyun acildiginda motoru otomatik baglar.", workX1, cardY, workX2 - workX1, autoInject);
                cardY += 55;
                drawConfigCard(event, "Glassmorphism Ultra", "Blur efektini maksimum duzeye getirir.", workX1, cardY, workX2 - workX1, glassmorphismUltra);
            }

            // INFOBAR
            int infoY = fy2 - 22;
            int pulseColor = (injectionStateCode == 2) ? 0xFF34D399 : (injectionStateCode == 1 ? 0xFFFBBF24 : 0xFF4B5563);
            event.getGuiGraphics().fill(workX1, infoY + 2, workX1 + 6, infoY + 8, pulseColor);
            event.getGuiGraphics().drawString(mc.font, injectionStatus, workX1 + 14, infoY, 0xFF888888, false);

            String lineCountStr = "Lines: " + editorLines.size() + " | UTF-8";
            event.getGuiGraphics().drawString(mc.font, lineCountStr, workX2 - mc.font.width(lineCountStr), infoY, 0xFF374151, false);
        }

        // --- KLON YARDIMCI METOTLAR ---
        private static void drawNavButton(RenderGuiEvent.Post event, String label, int x, int y, int w, int h, boolean isActive) {
            Minecraft mc = Minecraft.getInstance();
            if (isActive) {
                event.getGuiGraphics().fill(x, y, x + w, y + h, 0x14A855F7);
                event.getGuiGraphics().renderOutline(x, y, w, h, 0x40C084FC);
                event.getGuiGraphics().drawString(mc.font, label, x + 8, y + 7, 0xFFFFFFFF, true);
            } else {
                event.getGuiGraphics().drawString(mc.font, label, x + 8, y + 7, 0xFF4B5563, false);
            }
        }

        private static void drawConfigCard(RenderGuiEvent.Post event, String title, String desc, int x, int y, int w, boolean isToggleActive) {
            Minecraft mc = Minecraft.getInstance();
            int h = 46;
            event.getGuiGraphics().fill(x, y, x + w, y + h, 0x26151322);
            event.getGuiGraphics().renderOutline(x, y, w, h, 0x0FC084FC);
            event.getGuiGraphics().drawString(mc.font, title, x + 15, y + 10, 0xFFFFFFFF, false);
            event.getGuiGraphics().drawString(mc.font, desc, x + 15, y + 24, 0xFF4B5563, false);

            String btnText = isToggleActive ? "Aktif" : "Aktif Et";
            int btnW = mc.font.width(btnText) + 20;
            int bx = x + w - btnW - 15;
            int by = y + 13;
            event.getGuiGraphics().fill(bx, by, bx + btnW, by + 18, isToggleActive ? 0x33A855F7 : 0x1A6B7280);
            event.getGuiGraphics().renderOutline(bx, by, btnW, 18, isToggleActive ? 0xFFC084FC : 0x336B7280);
            event.getGuiGraphics().drawString(mc.font, btnText, bx + 10, by + 5, isToggleActive ? 0xFFC084FC : 0xFF9CA3AF, false);
        }

        // INTERAKTİF TIKLAMA REAKSİYONLARI
        @SubscribeEvent
        public static void onClientClick(ScreenEvent.MouseButtonPressed.Post event) {
            if (!isInterfaceOpen) return;

            Minecraft mc = Minecraft.getInstance();
            double mouseX = event.getMouseX() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getWidth();
            double mouseY = event.getMouseY() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getHeight();

            int pad = 30;
            int fx1 = pad, fy1 = pad;
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            int fx2 = width - pad, fy2 = height - pad;
            int sidebarWidth = 160;
            int sbX2 = fx1 + sidebarWidth;

            if (mouseX >= fx1 + 10 && mouseX <= sbX2 - 10) {
                int btnY = fy1 + 65;
                for (int i = 0; i < 4; i++) {
                    if (mouseY >= btnY && mouseY <= btnY + 22) { activeTab = i; return; }
                    btnY += 28;
                }
            }

            int workX1 = sbX2 + 20;
            int workY1 = fy1 + 20;
            int workX2 = fx2 - 20;

            if (activeTab == 0) {
                if (mouseY >= workY1 + 5 && mouseY <= workY1 + 20) {
                    if (mouseX >= workX2 - 210 && mouseX <= workX2 - 130) {
                        injectionStateCode = 1;
                        injectionStatus = "Injecting Amethyst Core...";
                        pendingStartTime = System.currentTimeMillis();
                    } else if (mouseX >= workX2 - 125 && mouseX <= workX2 - 60) {
                        editorLines.clear();
                    }
                }
            } else if (activeTab == 1) {
                int cardY = workY1 + 40;
                if (mouseX >= workX1 && mouseX <= workX2) {
                    if (mouseY >= cardY && mouseY <= cardY + 46) silentPacketMode = !silentPacketMode;
                    if (mouseY >= cardY + 55 && mouseY <= cardY + 101) velocityFilter = !velocityFilter;
                }
            } else if (activeTab == 3) {
                int cardY = workY1 + 40;
                if (mouseX >= workX1 && mouseX <= workX2) {
                    if (mouseY >= cardY && mouseY <= cardY + 46) autoInject = !autoInject;
                    if (mouseY >= cardY + 55 && mouseY <= cardY + 101) glassmorphismUltra = !glassmorphismUltra;
                }
            }
        }
    }
}