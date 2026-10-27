package com.curios.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CuriosFilesTest {
    @Test
    fun artifactsSlotsKeepCuriosTagAndGrowHands() {
        val loaded = loadCurios(
            listOf(
                SlotFile("curios", "head", """{"order":40,"validators":["curios:tag"]}"""),
                SlotFile("curios", "hands", """{"order":140,"validators":["curios:tag"]}"""),
                SlotFile("artifacts", "head", """{"add_cosmetic":true}"""),
                SlotFile("artifacts", "hands", """{"size":2,"add_cosmetic":true}"""),
                SlotFile("artifacts", "feet", """{"size":1,"order":190,"add_cosmetic":true}"""),
            ),
            listOf(
                EntityFile(
                    "artifacts",
                    "slots",
                    """{"entities":["player"],"slots":["head","hands","feet","missing"]}""",
                ),
            ),
        )
        val hands = loaded.slots.getValue("hands")
        assertEquals(2, hands.size)
        assertTrue(hands.addCosmetic)
        assertEquals(listOf("curios:tag"), hands.validators)
        assertEquals(listOf("head", "hands", "feet"), loaded.entitySlots["minecraft:player"])
        assertFalse("missing" in loaded.slots)
        assertTrue(
            validatorAllows(
                loaded.slots.getValue("feet").validators,
                "feet",
                setOf("curios:feet"),
            ),
        )
        assertTrue(validatorAllows(listOf("curios:tag"), "belt", setOf("curios:curio")))
        assertFalse(validatorAllows(listOf("curios:tag"), "belt", setOf("curios:ring")))
        assertFalse(validatorAllows(listOf("other:rule"), "belt", setOf("curios:belt")))
    }

    @Test
    fun trinketNamesMatchArtifactsAndBackpacks() {
        assertEquals("head" to "hat", trinketGroupAndName("head", 0))
        assertEquals("chest" to "necklace", trinketGroupAndName("necklace", 0))
        assertEquals("legs" to "belt", trinketGroupAndName("belt", 0))
        assertEquals("feet" to "shoes", trinketGroupAndName("feet", 0))
        assertEquals("chest" to "back", trinketGroupAndName("back", 0))
        assertEquals("hand" to "glove", trinketGroupAndName("hands", 0))
        assertEquals("offhand" to "glove", trinketGroupAndName("hands", 1))
    }

    @Test
    fun replaceClearsEarlierEntitySlots() {
        val loaded = loadCurios(
            listOf(SlotFile("curios", "ring", """{"order":1}"""), SlotFile("curios", "head", """{"order":2}""")),
            listOf(
                EntityFile("curios", "a", """{"entities":["minecraft:player"],"slots":["ring","head"]}"""),
                EntityFile("other", "b", """{"replace":true,"entities":["#player"],"slots":["head"]}"""),
            ),
        )
        assertEquals(listOf("head"), loaded.entitySlots["#minecraft:player"])
        assertEquals(listOf("ring", "head"), loaded.entitySlots["minecraft:player"])
    }
}
