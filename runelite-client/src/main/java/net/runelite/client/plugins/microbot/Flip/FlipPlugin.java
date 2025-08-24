/* Decompiler 40ms, total 262ms, lines 96 */
package net.runelite.client.plugins.microbot.Flip;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.AWTException;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.api.SchedulablePlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.PredicateCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.event.PluginScheduleEntrySoftStopEvent;

@PluginDescriptor(
   name = "<html>[<font color=#8A2BE2>Ω</font>] Flip",
   description = "Flipping copilot automation",
   tags = {"flip", "ge", "grand", "exchange", "automation"},
   enabledByDefault = false
)
public class FlipPlugin extends Plugin implements SchedulablePlugin {
   @Inject
   private FlipScript flipScript;

   @Provides
   FlipConfig provideConfig(ConfigManager configManager) {
      return (FlipConfig)configManager.getConfig(FlipConfig.class);
   }

   protected void startUp() throws AWTException {
      this.flipScript.run();
   }

   protected void shutDown() {
      this.flipScript.shutdown();
   }

   public LogicalCondition getStopCondition() {
      PredicateCondition<String> waitCondition = new PredicateCondition((suggestionType) -> {
         return "wait".equals(suggestionType);
      }, this::getCurrentSuggestionType, "Stop when suggestion type is 'wait'");
      return new OrCondition(new Condition[]{waitCondition});
   }

   public LogicalCondition getStartCondition() {
      PredicateCondition<String> notWaitCondition = new PredicateCondition((suggestionType) -> {
         return !"wait".equals(suggestionType);
      }, this::getCurrentSuggestionType, "Start when suggestion type is not 'wait'");
      return new OrCondition(new Condition[]{notWaitCondition});
   }

   private String getCurrentSuggestionType() {
      try {
         if (this.flipScript != null && this.flipScript.suggestionManager != null) {
            Object suggestion = this.flipScript.getSuggestion(this.flipScript.suggestionManager);
            return this.flipScript.getSuggestionType(suggestion);
         } else {
            return null;
         }
      } catch (Exception var2) {
         return null;
      }
   }

   @Subscribe
   public void onPluginScheduleEntrySoftStopEvent(PluginScheduleEntrySoftStopEvent event) {
      if (event.getPlugin() == this) {
         Microbot.getClientThread().invokeLater(() -> {
            try {
               Microbot.getPluginManager().setPluginEnabled(this, false);
               Microbot.getPluginManager().stopPlugin(this);
            } catch (Exception var2) {
               System.err.println("Error stopping FlipPlugin: " + var2.getMessage());
            }

         });
      }

   }

   public void onStopConditionCheck() {
      String currentType = this.getCurrentSuggestionType();
      if (currentType != null && System.currentTimeMillis() % 10000L < 1000L) {
         System.out.println("FlipperPlugin: Current suggestion type = " + currentType);
      }

   }

   public void reportFinished(String reason, boolean success) {
      SchedulablePlugin.super.reportFinished(reason, success);
   }
}
