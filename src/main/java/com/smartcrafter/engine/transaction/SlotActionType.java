package com.smartcrafter.engine.transaction;

/**
 * Types of safe client-side container slot interactions.
 */
public enum SlotActionType {
    /** Left-click: picks up or places whole cursor stack */
    PICKUP,
    /** Right-click: places a single item from cursor into slot */
    PLACE_ONE,
    /** Shift-click: quick transfers stack between container and inventory */
    QUICK_MOVE,
    /** Click slot to pick up craft result from result slot (slot 0) */
    TAKE_RESULT,
    /** Returns any item held by cursor back to safe inventory slot */
    RESTORE_CURSOR,
    /** Pause tick to allow server transaction acknowledgment */
    DELAY
}
