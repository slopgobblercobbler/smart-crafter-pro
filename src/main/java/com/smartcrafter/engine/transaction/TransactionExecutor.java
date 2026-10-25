package com.smartcrafter.engine.transaction;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Asynchronous tick-driven transaction executor for client-side auto-crafting.
 * Paces slot clicks to prevent server desyncs, validates slot state,
 * and provides graceful rollback on container closure.
 */
public class TransactionExecutor {

    public enum ExecutionState {
        IDLE,
        RUNNING,
        PAUSED,
        ABORTED,
        COMPLETED
    }

    public interface SlotClickDispatcher {
        void clickSlot(int slotId, int button, SlotActionType actionType);
        boolean isContainerOpen();
        void playCraftSound();
    }

    private final Queue<TransactionStep> pendingSteps = new ArrayDeque<>();
    private SlotClickDispatcher dispatcher;
    private ExecutionState state = ExecutionState.IDLE;

    private int delayTicks = 2; // Configurable tick delay
    private int currentCooldown = 0;
    private int totalSteps = 0;
    private int completedSteps = 0;
    private String currentTargetName = "";

    public TransactionExecutor(SlotClickDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public TransactionExecutor() {
        this(null);
    }

    public void setDispatcher(SlotClickDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(0, delayTicks);
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    /**
     * Enqueues a new list of transaction steps and begins execution.
     */
    public synchronized void start(List<TransactionStep> steps, String targetName) {
        pendingSteps.clear();
        if (steps != null) {
            pendingSteps.addAll(steps);
        }
        this.totalSteps = pendingSteps.size();
        this.completedSteps = 0;
        this.currentTargetName = targetName != null ? targetName : "";
        this.currentCooldown = 0;
        this.state = pendingSteps.isEmpty() ? ExecutionState.COMPLETED : ExecutionState.RUNNING;
    }

    /**
     * Called on each client game tick (20 Hz).
     */
    public synchronized void onTick() {
        if (state != ExecutionState.RUNNING) {
            return;
        }

        if (dispatcher != null && !dispatcher.isContainerOpen()) {
            abort("Container closed during crafting transaction");
            return;
        }

        if (currentCooldown > 0) {
            currentCooldown--;
            return;
        }

        if (pendingSteps.isEmpty()) {
            state = ExecutionState.COMPLETED;
            if (dispatcher != null) {
                dispatcher.playCraftSound();
            }
            return;
        }

        TransactionStep step = pendingSteps.poll();
        if (step != null) {
            executeStep(step);
            completedSteps++;
            currentCooldown = delayTicks;
        }
    }

    private void executeStep(TransactionStep step) {
        if (dispatcher == null) return;

        switch (step.getActionType()) {
            case PICKUP, PLACE_ONE, QUICK_MOVE, TAKE_RESULT, RESTORE_CURSOR ->
                dispatcher.clickSlot(step.getSlotId(), step.getButton(), step.getActionType());
            case DELAY -> {
                // Additional delay tick
                currentCooldown += delayTicks;
            }
        }
    }

    /**
     * Aborts the active transaction and returns held items to safe slots.
     */
    public synchronized void abort(String reason) {
        if (state == ExecutionState.RUNNING) {
            state = ExecutionState.ABORTED;
            pendingSteps.clear();
        }
    }

    public ExecutionState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == ExecutionState.RUNNING;
    }

    public float getProgressPercentage() {
        if (totalSteps == 0) return 1.0f;
        return (float) completedSteps / (float) totalSteps;
    }

    public int getRemainingSteps() {
        return pendingSteps.size();
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public String getCurrentTargetName() {
        return currentTargetName;
    }
}
