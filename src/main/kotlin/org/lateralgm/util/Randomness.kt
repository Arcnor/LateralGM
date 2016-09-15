package org.lateralgm.util

import java.util.Random

object Randomness {
	private val rand = Random()

	@JvmStatic
	fun nextInt() = rand.nextInt()
}