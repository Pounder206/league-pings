package com.lolpings;

import com.lolpings.LolPingsConfig.PingType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class LolPingsOverlay extends Overlay
{
    private final Client client;
    private final LolPingsPlugin plugin;
    private final LolPingsConfig config;

    // Increased stroke thickness for visibility
    private static final Stroke STROKE = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    // Thinner stroke for tile borders
    private static final Stroke TILE_BORDER_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    @Inject
    public LolPingsOverlay(Client client, LolPingsPlugin plugin, LolPingsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getPendingPings().isEmpty())
        {
            return null;
        }

        synchronized (plugin.getPendingPings())
        {
            Iterator<LolPing> it = plugin.getPendingPings().iterator();
            while (it.hasNext())
            {
                LolPing ping = it.next();

                // Remove ping if it has expired
                if (Instant.now().isAfter(ping.getTime().plus(config.pingDuration(), ChronoUnit.SECONDS)))
                {
                    it.remove();
                    continue;
                }

                renderPing(graphics, ping);
            }
        }

        return null;
    }

    private void renderPing(Graphics2D graphics, LolPing ping)
    {
        WorldPoint point = ping.getPoint();
        LocalPoint lp = LocalPoint.fromWorld(client, point);

        if (lp != null)
        {
            // Get position at plane 0 (ground level) for the circle
            Point canvasCenterGround = Perspective.localToCanvas(client, lp, 0);
            // Get position at plane 1 (above ground) for the ping graphic
            Point canvasCenter = Perspective.localToCanvas(client, lp, 1);

            if (canvasCenter == null || canvasCenterGround == null) return;

            int centerX = canvasCenter.getX();
            int centerY = canvasCenter.getY();
            int groundCenterX = canvasCenterGround.getX();
            int groundCenterY = canvasCenterGround.getY();

            // --- FADE-OUT LOGIC ---
            long totalDurationMs = config.pingDuration() * 1000L;
            long elapsedMs = ChronoUnit.MILLIS.between(ping.getTime(), Instant.now());

            // Calculate alpha factor: 1.0 (fully visible) to 0.0 (fully transparent)
            float alphaFactor = 1.0f - ((float) elapsedMs / totalDurationMs);
            int alpha = (int) (alphaFactor * 255);
            alpha = Math.max(0, Math.min(255, alpha));

            // --- BOBBING ANIMATION LOGIC ---
            long timeMs = System.currentTimeMillis();
            double period = 1000.0; // 1 second cycle duration
            double amplitude = 5.0; // 5 pixels maximum displacement

            // Calculate the vertical offset using a sine wave
            int bobOffset = (int) (amplitude * Math.sin(2 * Math.PI * timeMs / period));

            // Apply the bobbing offset to the center Y coordinate
            int drawCenterY = centerY + bobOffset;

            // Configuration for the shape dimensions
            final int Q_SIZE = 25;
            final int DOT_SIZE = 8;
            final int EXCLAMATION_HEIGHT = 25;

            // Save original stroke
            final Stroke originalStroke = graphics.getStroke();

            try
            {
                // Set the fading color based on ping type
                Color pingColor;
                switch (ping.getType())
                {
                    case QUESTION_MARK:
                        pingColor = new Color(255, 215, 0, alpha); // Gold
                        break;
                    case EXCLAMATION_MARK:
                        pingColor = new Color(255, 0, 0, alpha); // Red
                        break;
                    case SQUARE:
                        pingColor = new Color(0, 150, 255, alpha); // Blue
                        break;
                    case TRIANGLE:
                        pingColor = new Color(0, 255, 100, alpha); // Green
                        break;
                    case HORIZONTAL_LINE:
                        pingColor = new Color(255, 0, 255, alpha); // Magenta
                        break;
                    case CIRCLE:
                        pingColor = new Color(255, 165, 0, alpha); // Orange
                        break;
                    default:
                        pingColor = new Color(255, 255, 255, alpha); // White fallback
                        break;
                }

                // Render the filled tile at ground level (plane 0) - similar to party plugin
                renderTileFill(graphics, lp, pingColor);

                // Render the sender name in the center of the tile (first 4 letters)
                String senderName = ping.getSender();
                if (senderName != null && !senderName.isEmpty())
                {
                    String displayName = senderName.length() > 4 ? senderName.substring(0, 4) : senderName;
                    renderSenderName(graphics, groundCenterX, groundCenterY, displayName, pingColor);
                }

                // Set the stroke and color for the ping graphics (thick stroke)
                graphics.setColor(pingColor);
                graphics.setStroke(STROKE);

                // Render the ping graphic above ground (plane 1)
                switch (ping.getType())
                {
                    case QUESTION_MARK:
                        renderQuestionMark(graphics, centerX, drawCenterY, Q_SIZE, DOT_SIZE);
                        break;
                    case EXCLAMATION_MARK:
                        renderExclamationMark(graphics, centerX, drawCenterY, EXCLAMATION_HEIGHT, DOT_SIZE);
                        break;
                    case SQUARE:
                        renderSquare(graphics, centerX, drawCenterY);
                        break;
                    case TRIANGLE:
                        renderTriangle(graphics, centerX, drawCenterY);
                        break;
                    case HORIZONTAL_LINE:
                        renderHorizontalLine(graphics, centerX, drawCenterY);
                        break;
                    case CIRCLE:
                        renderCircle(graphics, centerX, drawCenterY);
                        break;
                }
            }
            finally
            {
                // Restore original stroke for other overlays
                graphics.setStroke(originalStroke);
            }
        }
    }

    private void renderQuestionMark(Graphics2D graphics, int centerX, int drawCenterY, int qSize, int dotSize)
    {
        // 1. Draw the top arc/curve of the question mark
        graphics.drawArc(
                centerX - qSize / 2,
                drawCenterY - qSize - 5,
                qSize,
                qSize,
                120,
                -210
        );

        // 2. Draw the stem (small straight line)
        graphics.drawLine(
                centerX,
                drawCenterY - 5,
                centerX,
                drawCenterY + 5
        );

        // 3. Draw the dot (solid circle)
        graphics.fillOval(
                centerX - dotSize / 2,
                drawCenterY + 10,
                dotSize,
                dotSize
        );
    }

    private void renderExclamationMark(Graphics2D graphics, int centerX, int drawCenterY, int height, int dotSize)
    {
        // 1. Draw the main vertical line
        graphics.drawLine(
                centerX,
                drawCenterY - height + 10, // Top point
                centerX,
                drawCenterY + 5 // Bottom of the line, above the dot
        );

        // 2. Draw the dot (solid circle)
        graphics.fillOval(
                centerX - dotSize / 2,
                drawCenterY + 10,
                dotSize,
                dotSize
        );
    }

    private void renderSenderName(Graphics2D graphics, int centerX, int centerY, String name, Color color)
    {
        // Save original rendering hints and font
        Object originalAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Font originalFont = graphics.getFont();

        try
        {
            // Enable text antialiasing for better readability
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Set font to bold and appropriate size
            Font nameFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            graphics.setFont(nameFont);
            graphics.setColor(color);

            // Get font metrics to center the text
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = metrics.stringWidth(name);
            int textHeight = metrics.getHeight();
            int textX = centerX - textWidth / 2;
            int textY = centerY + textHeight / 4; // Adjust for baseline

            // Draw the text
            graphics.drawString(name, textX, textY);
        }
        finally
        {
            // Restore original settings
            if (originalAntialiasing != null)
            {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, originalAntialiasing);
            }
            graphics.setFont(originalFont);
        }
    }

    private void renderSquare(Graphics2D graphics, int centerX, int centerY)
    {
        final int SIZE = 20;
        graphics.drawRect(
            centerX - SIZE / 2,
            centerY - SIZE / 2,
            SIZE,
            SIZE
        );
    }

    private void renderTriangle(Graphics2D graphics, int centerX, int centerY)
    {
        final int SIZE = 20;
        int[] xPoints = {
            centerX,                    // Top point
            centerX - SIZE / 2,         // Bottom left
            centerX + SIZE / 2          // Bottom right
        };
        int[] yPoints = {
            centerY - SIZE / 2,        // Top point
            centerY + SIZE / 2,        // Bottom left
            centerY + SIZE / 2         // Bottom right
        };
        graphics.drawPolygon(xPoints, yPoints, 3);
    }

    private void renderHorizontalLine(Graphics2D graphics, int centerX, int centerY)
    {
        final int LENGTH = 25;
        graphics.drawLine(
            centerX - LENGTH / 2,
            centerY,
            centerX + LENGTH / 2,
            centerY
        );
    }

    private void renderCircle(Graphics2D graphics, int centerX, int centerY)
    {
        final int RADIUS = 12;
        graphics.drawOval(
            centerX - RADIUS,
            centerY - RADIUS,
            RADIUS * 2,
            RADIUS * 2
        );
    }

    private void renderTileFill(Graphics2D graphics, LocalPoint lp, Color color)
    {
        // Get the tile polygon at plane 0 (ground level) using Perspective
        // This method gets the polygon for the tile at the specified local point
        java.awt.Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
        
        if (tilePoly != null && tilePoly.npoints > 0)
        {
            // Save original stroke
            Stroke originalStroke = graphics.getStroke();
            
            try
            {
                // Extract alpha from the ping color to maintain fade-out effect
                int alpha = color.getAlpha();
                
                // Fill the tile with a semi-transparent dark overlay to darken it
                // Use black with reduced alpha (about 30% of the ping's alpha for subtle darkening)
                int darkAlpha = (int) (alpha * 0.3);
                graphics.setColor(new Color(0, 0, 0, darkAlpha));
                graphics.fillPolygon(tilePoly);
                
                // Draw the colored border on top
                graphics.setColor(color);
                graphics.setStroke(TILE_BORDER_STROKE);
                graphics.drawPolygon(tilePoly);
            }
            finally
            {
                // Restore original stroke
                graphics.setStroke(originalStroke);
            }
        }
    }
}