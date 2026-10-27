package com.curios.mod

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CuriosLayoutTest {
    @Test
    fun twelveSlotsUseTwoColumnsBesideTheInventory() {
        assertEquals(2, curioColumns(12))
        assertEquals(51, curioPanelWidth(12))
        assertEquals(122, curioPanelHeight(12))
        assertEquals(-51, curioPanelX(12))
        assertEquals(6, curioRowCount(12))
        assertEquals(-25 to 8, curioInventoryPosition(0, 12))
        assertEquals(-43 to 8, curioInventoryPosition(1, 12))
        assertEquals(-25 to 26, curioInventoryPosition(2, 12))
    }

    @Test
    fun clicksOnTheSidePanelStayInsideTheScreen() {
        val left = 100
        val top = 40
        assertTrue(curioPanelContains(left - 17.0, top + 16.0, left, top, 7))
        assertFalse(curioPanelContains(left - 40.0, top + 16.0, left, top, 7))
        assertFalse(curioPanelContains(left + 20.0, top + 16.0, left, top, 7))
        assertFalse(curioPanelContains(left - 17.0, top + 16.0, left, top, 0))
    }

    @Test
    fun equippedStackStaysVisibleWhenTheDatapackListIsEmpty() {
        val stored = listOf(SlotRef("back", 0, false))
        assertEquals(stored, visibleSlotRefs(emptyList(), stored))
        assertEquals(stored, visibleSlotRefs(stored, stored))
    }

    @Test
    fun storedKeyKeepsTheSlotAndTheCosmeticFlag() {
        assertEquals(SlotRef("necklace", 1, false), slotRef("necklace", 1))
        assertEquals(SlotRef("head", 0, true), slotRef("head#cosmetic", 0))
    }

    @Test
    fun overflowSlotsStayOffTheInventory() {
        assertEquals(-2000 to -2000, curioInventoryPosition(24, 30))
        assertEquals(0, curioPanelWidth(0))
    }
}
