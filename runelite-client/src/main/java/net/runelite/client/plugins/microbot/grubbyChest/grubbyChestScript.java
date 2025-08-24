package net.runelite.client.plugins.microbot.grubbyChest;

import com.google.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;


import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.grubbyChest.grubbyChestStates.BANKING;

public class grubbyChestScript extends Script {

    public static grubbyChestStates State;
    public final grubbyChestPlugin plugin;

    @Inject
    public grubbyChestScript(grubbyChestPlugin plugin) {
        this.plugin = plugin;
    }
    private static final WorldPoint MID_POINT = new WorldPoint(1830, 9973, 0);
    private static final WorldPoint CHEST_LOCATION = new WorldPoint(1795, 9925, 0);
    private static final WorldArea CHEST_AREA = new WorldArea(1796, 9924, 2, 3, 0);

//    private static final WorldPoint DOOR_LOCATION = new WorldPoint(1798, 9925, 0);

    private static final int GRUBBY_KEY = 23499;
    //    static final int KR_BANKBOOTH = 25808;
//    private static final int GRUBBY_DOOR_ID = 34840; private
    private static final int GRUBBY_CHEST_CLOSED = 34901;
//    private long lastActionTime = System.currentTimeMillis();

//    private void updateIdleTimer() {
//        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
//            lastActionTime = System.currentTimeMillis();
//        }
//    }
//
//    private void checkIdleCondition() {
//        if (System.currentTimeMillis() - lastActionTime > 5000) {
//            grubbyChestStates state = getState();
//        }
//    }
    private grubbyChestStates getState() {

        if (!Rs2Inventory.hasItem(GRUBBY_KEY)) {
            return BANKING;
        }

        if (Rs2Player.getRs2WorldPoint().getWorldPoint().isInArea(CHEST_AREA) && Rs2Inventory.hasItem(GRUBBY_KEY)) {
            return grubbyChestStates.LOOTING;
        }

        return grubbyChestStates.RUNTOGRUBBYCHEST;
    }

//    private static final long WALK_TO_BANK_TIMEOUT_MS = 10_000;

    public boolean run(grubbyChestConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                long startTime = System.currentTimeMillis();


                grubbyChestStates currentState = getState();
                GameObject chest = Rs2GameObject.getGameObject(GRUBBY_CHEST_CLOSED, CHEST_LOCATION);
//                updateIdleTimer();
//                checkIdleCondition();


                switch (currentState) {
                    case BANKING:
                        Microbot.status = "Banking";

                        while (!Rs2Player.isFullHealth()) {
                            Rs2Player.eatAt(100);
                            sleep(100,500);
                        }

//                        if (!Rs2Bank.walkToBankAndUseBank(BankLocation.HOSIDIUS)) {
//                            sleepUntil(Rs2Bank::isOpen);
//                            Microbot.log("Failed to walk to bank.");
//                            stop();
//                            return;
//                        }

                        if (!Rs2Inventory.hasItem(GRUBBY_KEY) && !Rs2Bank.isOpen()) {
                            Rs2Bank.walkToBankAndUseBank(BankLocation.HOSIDIUS);
                            sleepUntil(Rs2Bank::isOpen);
                        } else {
                            Microbot.log("Unable to open bank. Stopping.");
                            stop();
                            return;
                        }

                        if (Rs2Player.getRunEnergy() <= 70) {
                            Rs2Bank.withdrawX("energy potion",2);
                        }
                        while (Rs2Player.getRunEnergy() <= 70) {
                            sleepUntil(() -> Rs2Inventory.hasItem("energy potion"));
                            Rs2Inventory.interact("energy potion", "drink");
                            sleepGaussian(600, 3);
                        }
                        if (Rs2Player.hasStaminaActive()) {
                            Rs2Bank.depositAll();
                            sleepGaussian(500,3);
                            Rs2Bank.withdrawX(GRUBBY_KEY,2);
                            sleepUntil(() ->Rs2Inventory.hasItemAmount(GRUBBY_KEY,2));
                            Rs2Bank.closeBank();
                        }
                        else Rs2Bank.withdrawOne("stamina potion");
                        sleepUntil(() -> Rs2Inventory.hasItem("stamina potion"));
                        Rs2Inventory.interact("stamina potion", "drink");
                        sleep(100,1000);
                        Rs2Bank.depositAll();
                        sleep(100,1000);
                        Rs2Bank.withdrawX(GRUBBY_KEY,2);
                        sleep(100,1000);
                        Rs2Bank.withdrawX(6689,1);
                        sleep(100,1000);
                        Rs2Bank.closeBank();


                        currentState = grubbyChestStates.RUNTOGRUBBYCHEST;
                        break;

                    case RUNTOGRUBBYCHEST:
                        Microbot.status = "Running to Grubby Chest";

                        while (Rs2Player.getHealthPercentage() < 50) {
                            Rs2Player.useFood();
                            if(Rs2Inventory.hasItemAmount(GRUBBY_KEY,2)){
                            Rs2Walker.walkTo(MID_POINT);
                            sleep(100,1000);

                                Rs2Walker.walkNextTo(chest);
                                sleepUntil(() -> Rs2Player.getRs2WorldPoint().getWorldPoint().isInArea(CHEST_AREA));

                            // Pick-lock the door
//                        GameObject door = Rs2GameObject.getGameObject(GRUBBY_DOOR_ID, DOOR_LOCATION);
//                        if (door != null) {
//                            Rs2GameObject.interact(door, "Pick-lock");
//                            sleep(2000, 3000);
                           }
                        }
                        currentState = grubbyChestStates.LOOTING;
                        break;

                    case LOOTING:
                        Microbot.status = "Looting Chest";

                        if (chest == null) {
                            Microbot.log("Chest not found at location.");
                            stop();
                            return;
                        }

                        if (!Rs2Inventory.hasItem(GRUBBY_KEY)) {
                            Microbot.log("No Grubby Key in inventory.");
                            stop();
                            return;
                        }


                        while (Rs2Inventory.hasItem(GRUBBY_KEY)) {
                            Rs2GameObject.interact(chest, "Open");
                            sleep(1200, 2000);
                        }

                        currentState = BANKING;
                        break;
                }

                long endTime = System.currentTimeMillis();
                Microbot.log("Loop time:" + (endTime - startTime) + " ms");

            } catch (Exception ex) {
                Microbot.log("Error in grubbyChestScript: " + ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void stop() {
        Microbot.log("Stopping grubbyChestScript.");
        shutdown();
    }
}

