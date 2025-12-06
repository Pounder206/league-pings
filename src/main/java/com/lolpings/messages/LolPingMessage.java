package com.lolpings.messages;

import com.lolpings.LolPingsConfig.PingType;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.messages.PartyMessage;

/**
 * Custom ping message for League of Legends style pings.
 * This is separate from TilePing to avoid conflicts with the party plugin.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class LolPingMessage extends PartyMessage
{
	WorldPoint point;
	PingType pingType;
	String sender;
}

