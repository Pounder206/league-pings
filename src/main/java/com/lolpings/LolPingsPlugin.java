package com.lolpings;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.lolpings.LolPingsConfig.PingType;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.SoundEffectID;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import com.lolpings.messages.LolPingMessage;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
        name = "League of Legends Pings",
        description = "Replaces generic party pings with LoL style pings",
        tags = {"party", "ping", "lol", "league"}
)
public class LolPingsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private PartyService party;

    @Inject
    private WSClient wsClient;

    @Inject
    private LolPingsConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private LolPingsOverlay overlay;

    @Inject
    private KeyManager keyManager;

    // Thread-safe list to store active pings
    @Getter
    private final List<LolPing> pendingPings = Collections.synchronizedList(new ArrayList<>());

    // Set to null as image loading is no longer used
    @Getter
    private BufferedImage questionMarkImage = null;

    private final HotkeyListener questionMarkHotkeyListener = new HotkeyListener(() -> config.questionMarkHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            questionMarkHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            questionMarkHotkeyPressed = false;
        }
    };

    private final HotkeyListener exclamationMarkHotkeyListener = new HotkeyListener(() -> config.exclamationMarkHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            exclamationMarkHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            exclamationMarkHotkeyPressed = false;
        }
    };

    private final HotkeyListener squareHotkeyListener = new HotkeyListener(() -> config.squareHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            squareHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            squareHotkeyPressed = false;
        }
    };

    private final HotkeyListener triangleHotkeyListener = new HotkeyListener(() -> config.triangleHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            triangleHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            triangleHotkeyPressed = false;
        }
    };

    private final HotkeyListener horizontalLineHotkeyListener = new HotkeyListener(() -> config.horizontalLineHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            horizontalLineHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            horizontalLineHotkeyPressed = false;
        }
    };

    private final HotkeyListener circleHotkeyListener = new HotkeyListener(() -> config.circleHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            circleHotkeyPressed = true;
        }

        @Override
        public void hotkeyReleased()
        {
            circleHotkeyPressed = false;
        }
    };

    private boolean questionMarkHotkeyPressed = false;
    private boolean exclamationMarkHotkeyPressed = false;
    private boolean squareHotkeyPressed = false;
    private boolean triangleHotkeyPressed = false;
    private boolean horizontalLineHotkeyPressed = false;
    private boolean circleHotkeyPressed = false;

    @Override
    protected void startUp() throws Exception
    {
        // Image loading removed to rely on procedural drawing
        questionMarkImage = null;

        overlayManager.add(overlay);
        keyManager.registerKeyListener(questionMarkHotkeyListener);
        keyManager.registerKeyListener(exclamationMarkHotkeyListener);
        keyManager.registerKeyListener(squareHotkeyListener);
        keyManager.registerKeyListener(triangleHotkeyListener);
        keyManager.registerKeyListener(horizontalLineHotkeyListener);
        keyManager.registerKeyListener(circleHotkeyListener);

        // Register our custom LolPingMessage so we can receive pings from other party members
        wsClient.registerMessage(LolPingMessage.class);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(questionMarkHotkeyListener);
        keyManager.unregisterKeyListener(exclamationMarkHotkeyListener);
        keyManager.unregisterKeyListener(squareHotkeyListener);
        keyManager.unregisterKeyListener(triangleHotkeyListener);
        keyManager.unregisterKeyListener(horizontalLineHotkeyListener);
        keyManager.unregisterKeyListener(circleHotkeyListener);
        wsClient.unregisterMessage(LolPingMessage.class);
        pendingPings.clear();
    }

    @Subscribe
    public void onLolPingMessage(LolPingMessage event)
    {
        // This handles pings received from other party members using this plugin.
        // Since we use our own message type, we won't receive party plugin's TilePing messages.
        WorldPoint pingPoint = event.getPoint();
        PingType pingType = event.getPingType();
        String sender = event.getSender();
        Instant now = Instant.now();
        
        // Play sound if enabled
        playPingSound();
        
        // Display the ping from another party member using the type sent by the broadcaster
        pendingPings.add(new LolPing(pingPoint, now, pingType, sender));
    }

    @Subscribe(priority = 100) // High priority to process before party plugin
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        // Determine which hotkey is currently pressed and what ping type to send
        PingType pingTypeToSend = null;
        if (questionMarkHotkeyPressed)
        {
            pingTypeToSend = PingType.QUESTION_MARK;
        }
        else if (exclamationMarkHotkeyPressed)
        {
            pingTypeToSend = PingType.EXCLAMATION_MARK;
        }
        else if (squareHotkeyPressed)
        {
            pingTypeToSend = PingType.SQUARE;
        }
        else if (triangleHotkeyPressed)
        {
            pingTypeToSend = PingType.TRIANGLE;
        }
        else if (horizontalLineHotkeyPressed)
        {
            pingTypeToSend = PingType.HORIZONTAL_LINE;
        }
        else if (circleHotkeyPressed)
        {
            pingTypeToSend = PingType.CIRCLE;
        }
        else
        {
            // No ping hotkey is pressed, ignore this click
            return;
        }

        // Check if menu is open or not in party
        if (client.isMenuOpen() || !party.isInParty())
        {
            return;
        }

        // Check if the click was on the scene (Walk here)
        boolean isOnCanvas = false;
        for (MenuEntry menuEntry : client.getMenuEntries())
        {
            if (menuEntry == null) continue;
            if ("walk here".equalsIgnoreCase(menuEntry.getOption()))
            {
                isOnCanvas = true;
                break;
            }
        }

        if (!isOnCanvas) return;

        Tile selectedSceneTile = client.getSelectedSceneTile();
        if (selectedSceneTile == null)
        {
            return;
        }

        event.consume(); // Prevent the actual walk action and prevent party plugin from processing it

        WorldPoint pingLocation = selectedSceneTile.getWorldLocation();
        Instant pingTime = Instant.now();

        // Get the local player's name to include as sender
        String senderName = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName()
            : "Unknown";

        // Send our custom ping message to the party so other members using this plugin can see it
        // Include the ping type based on which hotkey was pressed and the sender's name
        final LolPingMessage lolPingMessage = new LolPingMessage(pingLocation, pingTypeToSend, senderName);
        party.send(lolPingMessage);

        // Play sound if enabled
        playPingSound();

        // Add the ping to the local list immediately so the sender sees their own ping
        pendingPings.add(new LolPing(pingLocation, pingTime, pingTypeToSend, senderName));
    }

    private void playPingSound()
    {
        if (config.pingSound())
        {
            clientThread.invoke(() -> client.playSoundEffect(SoundEffectID.UI_BOOP));
        }
    }

    @Provides
    LolPingsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LolPingsConfig.class);
    }

}