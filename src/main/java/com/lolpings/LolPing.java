package com.lolpings;

import java.time.Instant;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import com.lolpings.LolPingsConfig.PingType;

@Getter
public class LolPing
{
    private final WorldPoint point;
    private final Instant time;
    private final PingType type;
    private final String sender;

    public LolPing(WorldPoint point, Instant time, PingType type, String sender)
    {
        this.point = point;
        this.time = time;
        this.type = type;
        this.sender = sender;
    }

}