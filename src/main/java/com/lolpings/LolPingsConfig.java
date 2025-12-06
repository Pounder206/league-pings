package com.lolpings;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("lolpings")
public interface LolPingsConfig extends Config
{
    enum PingType
    {
        QUESTION_MARK("?"),
        EXCLAMATION_MARK("!"),
        SQUARE("■"),
        TRIANGLE("▲"),
        HORIZONTAL_LINE("─"),
        CIRCLE("●");

        private final String icon;

        PingType(String icon)
        {
            this.icon = icon;
        }

        @Override
        public String toString()
        {
            return icon;
        }
    }

    @ConfigItem(
            keyName = "questionMarkHotkey",
            name = "Question Mark Ping Hotkey",
            description = "Key to hold to send a question mark ping (e.g. Z)"
    )
    default Keybind questionMarkHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "exclamationMarkHotkey",
            name = "Exclamation Mark Ping Hotkey",
            description = "Key to hold to send an exclamation mark ping (e.g. X)"
    )
    default Keybind exclamationMarkHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "squareHotkey",
            name = "Square Ping Hotkey",
            description = "Key to hold to send a square ping"
    )
    default Keybind squareHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "triangleHotkey",
            name = "Triangle Ping Hotkey",
            description = "Key to hold to send a triangle ping"
    )
    default Keybind triangleHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "horizontalLineHotkey",
            name = "Horizontal Line Ping Hotkey",
            description = "Key to hold to send a horizontal line ping"
    )
    default Keybind horizontalLineHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "circleHotkey",
            name = "Circle Ping Hotkey",
            description = "Key to hold to send a circle ping"
    )
    default Keybind circleHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "pingDuration",
            name = "Ping Duration",
            description = "How long the ping stays visible (seconds)"
    )
    default int pingDuration()
    {
        return 3;
    }

    @ConfigItem(
            keyName = "pingSound",
            name = "Ping Sound",
            description = "Play a sound effect when a ping is sent or received"
    )
    default boolean pingSound()
    {
        return true;
    }
}