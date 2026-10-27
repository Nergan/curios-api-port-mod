package com.curios.mod

import net.minecraft.world.inventory.Slot

const val VANILLA_INVENTORY_WIDTH = 176
const val CURIO_COLUMN_ROWS = 8

fun curioColumns(count: Int): Int =
    if (count <= 0) 0 else ((count + CURIO_COLUMN_ROWS - 1) / CURIO_COLUMN_ROWS).coerceAtMost(3)

fun curioPanelWidth(count: Int): Int {
    val columns = curioColumns(count)
    if (columns == 0) return 0
    return 33 + (columns - 1) * 18
}

fun curioPanelHeight(count: Int): Int {
    val rows = curioRowCount(count)
    if (rows == 0) return 0
    return 14 + rows * 18
}

fun curioPanelX(count: Int): Int {
    val columns = curioColumns(count)
    if (columns == 0) return 0
    return -33 - (columns - 1) * 18
}

fun curioPanelContains(mouseX: Double, mouseY: Double, left: Int, top: Int, count: Int): Boolean {
    val width = curioPanelWidth(count)
    val height = curioPanelHeight(count)
    if (width <= 0 || height <= 0) return false
    val x0 = left + curioPanelX(count)
    val y0 = top
    return mouseX >= x0 && mouseY >= y0 && mouseX < x0 + width && mouseY < y0 + height
}

fun curioRowCount(count: Int): Int {
    val columns = curioColumns(count)
    if (columns == 0) return 0
    return ((count + columns - 1) / columns).coerceAtMost(CURIO_COLUMN_ROWS)
}

fun curioInventoryPosition(index: Int, count: Int): Pair<Int, Int> {
    val columns = curioColumns(count)
    if (columns == 0) return -2000 to -2000
    val row = index / columns
    if (row >= CURIO_COLUMN_ROWS) return -2000 to -2000
    val column = index % columns
    // Column 0 sits against the inventory. Further columns step left by one slot.
    return (-25 - column * 18) to (8 + row * 18)
}

fun curioCount(slots: List<Slot>): Int = slots.count { it is CurioSlot }
