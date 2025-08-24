package net.runelite.client.plugins.microbot.Flip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;
import java.awt.event.KeyEvent;
import java.awt.Rectangle;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;

@Slf4j
public class FlipScript extends Script {
	private final WorldArea grandExchangeArea = new WorldArea(3136, 3465, 61, 54, 0);

    private Plugin flippingCopilot;
	protected Object suggestionManager;
    private Object highlightController;
    private long lastActionTime = 0;
    private long actionCooldown = 2000; // randomized cooldown between actions
	private long lastMoveMouseOffScreen = 0;
	private long timeBetweenMoveMouseOffScreenActions = 30000;//default to 30 seconds, can maybe make it user configurable

	private int[] grandExchangeSlotIds = new int[] {
		InterfaceID.GeOffers.INDEX_0,
		InterfaceID.GeOffers.INDEX_1,
		InterfaceID.GeOffers.INDEX_2,
		InterfaceID.GeOffers.INDEX_3,
		InterfaceID.GeOffers.INDEX_4,
		InterfaceID.GeOffers.INDEX_5,
		InterfaceID.GeOffers.INDEX_6,
		InterfaceID.GeOffers.INDEX_7
	};

    public boolean run() {
        Rs2AntibanSettings.naturalMouse = true;
		Rs2AntibanSettings.microBreakChance = 0.1;
		Rs2AntibanSettings.moveMouseOffScreen = true;
		Rs2AntibanSettings.moveMouseOffScreenChance = 0.05;
        Rs2Antiban.setActivityIntensity(ActivityIntensity.VERY_LOW);
		
            mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
				if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                if (!initialize()) {
					log.warn("FlipScript initialization failed. Ensure Flipping Copilot is installed and enabled.");
					return;
				}
						if(!"wait".equals(getSuggestionType(getSuggestion(suggestionManager)))
							&& !BreakHandlerScript.isLockState()){
								BreakHandlerScript.setLockState(true);
							}

						if(BreakHandlerScript.isMicroBreakActive()) sleepUntil(() -> !BreakHandlerScript.isBreakActive(), 120000);

                        if (!grandExchangeArea.contains(Microbot.getClient().getLocalPlayer().getWorldLocation())) {
                            Rs2GrandExchange.walkToGrandExchange();
                        }

                        if (!Rs2GrandExchange.isOpen()) {
                            Rs2GrandExchange.openExchange();
                            return;
                        }

						// Check for Copilot price/quantity messages in chat
						if (checkAndPressCopilotKeybind()) return;

                        // Check if we need to abort any offers
                        if (checkAndAbortIfNeeded()) return;

                        // Check for highlighted widgets
                        checkAndClickHighlightedWidgets();

						//anti-ban features
						if("wait".equals(getSuggestionType(getSuggestion(suggestionManager)))){
							
							if((System.currentTimeMillis() - lastMoveMouseOffScreen)> timeBetweenMoveMouseOffScreenActions){
								 Rs2Antiban.moveMouseOffScreen();
								 lastMoveMouseOffScreen = System.currentTimeMillis();
							}
							BreakHandlerScript.setLockState(false);

							sleepUntil(() -> !"wait".equals(getSuggestionType(getSuggestion(suggestionManager))), 60000);
							sleep(200, 100);
						}
						else{
						Rs2Antiban.moveMouseOffScreen(0.05);
						BreakHandlerScript.setLockState(true);
						}
						Rs2Antiban.takeMicroBreakByChance();
            } catch (Exception ex) {
                log.error("Error in FlipScript: {} - ", ex.getMessage(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        return true;
    }

	@Override
	public void shutdown()
	{
		flippingCopilot = null;
		suggestionManager = null;
		highlightController = null;
		lastActionTime = 0;
		actionCooldown = 2000;
		lastMoveMouseOffScreen = 0;
		Rs2Antiban.resetAntibanSettings();
		super.shutdown();
	}

	private boolean initialize()
	{
		if (flippingCopilot != null && suggestionManager != null && highlightController != null) return true;

		Plugin _flippingCopilot = getFlippingCopilot();
		Object _suggestionManager = getSuggestionManager(_flippingCopilot);
		Object _highlightController = getHighlightController(_flippingCopilot);

		return _flippingCopilot != null && _suggestionManager != null && _highlightController != null;
	}

	private Plugin getFlippingCopilot()
	{
		if (flippingCopilot == null)
		{
			flippingCopilot = Microbot.getPluginManager()
				.getPlugins()
				.stream()
				.filter(plugin -> plugin.getClass().getSimpleName().equalsIgnoreCase("FlippingCopilotPlugin"))
				.findFirst()
				.orElse(null);
		}
		return flippingCopilot;
	}

	private Object getHighlightController(Plugin flippingCopilot)
	{
		if (flippingCopilot == null) return null;
		if (highlightController == null)
		{
			try
			{
				Field highlightControllerField = flippingCopilot.getClass().getDeclaredField("highlightController");
				highlightControllerField.setAccessible(true);
				highlightController = highlightControllerField.get(flippingCopilot);
			}
			catch (Exception e)
			{
				log.error("Could not access HighlightController: {} - ", e.getMessage(), e);
			}
		}
		return highlightController;
	}

	private Object getSuggestionManager(Plugin flippingCopilot)
	{
		if (flippingCopilot == null) return null;
		if (suggestionManager == null)
		{
			try
			{
				Field suggestionManagerField = flippingCopilot.getClass().getDeclaredField("suggestionManager");
				suggestionManagerField.setAccessible(true);
				suggestionManager = suggestionManagerField.get(flippingCopilot);
			}
			catch (Exception e)
			{
				log.error("Could not access SuggestionManager: {} - ", e.getMessage(), e);
			}
		}
		return suggestionManager;
	}

	protected Object getSuggestion(Object suggestionManager)
	{
		if (suggestionManager == null) return null;
		try
		{
			Field suggestionField = suggestionManager.getClass().getDeclaredField("suggestion");
			suggestionField.setAccessible(true);
			return suggestionField.get(suggestionManager);
		}
		catch (Exception e)
		{
			log.error("Could not access Suggestion: {} ", e.getMessage(), e);
			return null;
		}
	}

	protected String getSuggestionType(Object suggestion)
	{
		if (suggestion == null) return null;
		try
		{
			Field typeField = suggestion.getClass().getDeclaredField("type");
			typeField.setAccessible(true);
			return (String) typeField.get(suggestion);
		}
		catch (Exception e)
		{
			log.error("Could not access suggestion type: {} - ", e.getMessage(), e);
			return null;
		}
	}

	private List<Object> getHighlightOverlays(Object highlightController)
	{
		if (highlightController == null) return null;
		try
		{
			Field highlightOverlaysField = highlightController.getClass().getDeclaredField("highlightOverlays");
			highlightOverlaysField.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<Object> highlightOverlays = (List<Object>) highlightOverlaysField.get(highlightController);
			return highlightOverlays;
		}
		catch (Exception e)
		{
			log.error("Could not access highlight overlays: {} - ", e.getMessage(), e);
			return null;
		}
	}

	private List<Widget> getHighlightWidgets(Object highlightController)
	{
		final List<Widget> highlightWidgets = new ArrayList<>();
		if (highlightController == null)
		{
			return highlightWidgets;
		}

		List<Object> highlightOverlays = getHighlightOverlays(highlightController);

		for (Object highlightOverlay : highlightOverlays)
		{
			try
			{
				Field widgetField = highlightOverlay.getClass().getDeclaredField("widget");
				widgetField.setAccessible(true);
				highlightWidgets.add((Widget) widgetField.get(highlightOverlay));
			}
			catch (Exception e)
			{
				log.error("Could not get widget from overlay: {} - ", e.getMessage(), e);
				return highlightWidgets;
			}
		}

		return highlightWidgets;
	}

	private Widget getWidgetFromOverlay(Object highlightController, String suggestionType)
	{
		List<Object> highlightOverlays = getHighlightOverlays(highlightController);
		if (highlightOverlays == null || highlightOverlays.isEmpty())
		{
			return null;
		}

		if (Objects.equals(suggestionType, "abort"))
		{
			return getHighlightWidgets(highlightController).stream()
				.filter(Objects::nonNull)
				// Filter to "home" grand exchange slot widgets
				.filter(widget -> Arrays.stream(grandExchangeSlotIds).anyMatch(id -> id == widget.getId()))
				.findFirst()
				.orElse(null);
		}
		else
		{
			// For other suggestion types, we can return the first highlighted widget
			return getHighlightWidgets(highlightController).stream()
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
		}
	}

	private boolean checkAndAbortIfNeeded()
	{
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastActionTime < actionCooldown) return true;

		if (flippingCopilot == null || highlightController == null || suggestionManager == null) return false;
		try
		{
			Object currentSuggestion = getSuggestion(suggestionManager);
			if (currentSuggestion == null) return false;

			String suggestionType = getSuggestionType(currentSuggestion);

			if (!Objects.equals(suggestionType, "abort")) return false;

			log.info("Found suggestion type '{}'.", suggestionType);

			Widget abortWidget = getWidgetFromOverlay(highlightController, suggestionType);
			if (abortWidget != null)
			{
				NewMenuEntry menuEntry = new NewMenuEntry("Abort offer", "", 2, MenuAction.CC_OP, 2, abortWidget.getId(), false);
				Rectangle bounds = abortWidget.getBounds() != null && Rs2UiHelper.isRectangleWithinCanvas(abortWidget.getBounds())
					? abortWidget.getBounds()
					: Rs2UiHelper.getDefaultRectangle();
				Microbot.doInvoke(menuEntry, bounds);
				lastActionTime = System.currentTimeMillis();
				actionCooldown = Rs2Random.randomGaussian(1200, 300);
				return true;
			}
		}
		catch (Exception e)
		{
			log.error("Could not process suggestion: {} - ", e.getMessage(), e);
		}
		return false;
	}

    private boolean checkAndPressCopilotKeybind() {
		boolean isChatboxInputOpen = Rs2Widget.isWidgetVisible(InterfaceID.Chatbox.MES_LAYER);
		if (!isChatboxInputOpen) return false;
		log.info("Found chatbox input open, pressing 'e' then Enter");
		// Press 'e' to trigger FlippingCopilot's keybind
		Rs2Keyboard.keyPress(KeyEvent.VK_E);
		sleep(250, 400);
		Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);

		lastActionTime = System.currentTimeMillis();
		return true;
    }

    private void checkAndClickHighlightedWidgets()
	{
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastActionTime < actionCooldown) return;

		if (flippingCopilot == null || highlightController == null) return;

		try {
			Widget highlightedWidget = getWidgetFromOverlay(highlightController, "");
			boolean isHighlightedVisible = highlightedWidget != null && Rs2Widget.isWidgetVisible(highlightedWidget.getId());

			if (isHighlightedVisible) {
				log.info("Clicking highlighted widget: {}", highlightedWidget.getId());
				Rs2Widget.clickWidget(highlightedWidget);
				lastActionTime = currentTime;
                actionCooldown = Rs2Random.randomGaussian(1800, 300);
			}
		}
		catch (Exception e)
		{
			log.error("Could not process highlight widgets: {} - ", e.getMessage(), e);
		}
	}
}