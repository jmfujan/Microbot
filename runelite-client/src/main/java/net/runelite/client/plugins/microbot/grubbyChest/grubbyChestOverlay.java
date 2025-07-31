package net.runelite.client.plugins.microbot.grubbyChest;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class grubbyChestOverlay extends OverlayPanel {
    private final grubbyChestPlugin plugin;
    public final ButtonComponent myButton;

    @Inject
    grubbyChestOverlay(grubbyChestPlugin plugin) {
        super(plugin);
        this.plugin = plugin; // ✅ assign plugin here

        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();

        myButton = new ButtonComponent("Test");
        myButton.setPreferredSize(new Dimension(100, 30));
        myButton.setParentOverlay(this);
        myButton.setFont(FontManager.getRunescapeBoldFont());
        myButton.setOnClick(() -> Microbot.openPopUp(
                "Microbot",
                String.format("S-1D:<br><br><col=ffffff>%s Popup</col>", "IDK bro")
        ));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.setBackgroundColor(Color.cyan);
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Thunderstick420 Grubby v2.1.4")
                    .color(Color.BLACK)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(Microbot.status)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Chests Opened:")
                    .right(String.valueOf(plugin.chestsOpened))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time Running:")
                    .right(plugin.getTimeRunning())
                    .build());

            Duration runTime = Duration.between(plugin.scriptStartTime, Instant.now());
            if (runTime.getSeconds() > 0) {
                double hoursElapsed = runTime.toMillis() / (1000.0 * 60.0 * 60.0);
                int chestsPerHour = (int) (plugin.chestsOpened / hoursElapsed);

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Chests/hr:")
                        .right(String.valueOf(chestsPerHour))
                        .build());
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
