/**
 * @file MarkerCache.kt
 * *
 * @brief Class implementing token marker caching for efficiency.
 * *
 * @section License
 * *
 *
 *
 * * Copyright (C) 2013 Robert B. Colton
 * * Copyright (C) 2016 Edu Garcia
 * * This file is a part of the LateralGM IDE.
 * *
 *
 *
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation, either version 3 of the License, or
 * * (at your option) any later version.
 * *
 *
 *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 *
 *
 * * You should have received a copy of the GNU General Public License
 * * along with this program. If not, see //www.gnu.org/licenses/>.
 */

package org.lateralgm.components

import org.lateralgm.joshedit.DefaultTokenMarker
import org.lateralgm.joshedit.lexers.GLESTokenMarker
import org.lateralgm.joshedit.lexers.GLSLTokenMarker
import org.lateralgm.joshedit.lexers.GMLTokenMarker
import org.lateralgm.joshedit.lexers.HLSLTokenMarker

import java.util.EnumMap
import java.util.HashMap

/**
 * A non-instantiable token marker cache that avoids instantiating more
 * markers than is necessary.

 * @author Robert B. Colton
 */
object MarkerCache {
	enum class Language {
		GLSLES, GLSL, GML, HLSL
	}

	/** The cached token markers keyed with the language  */
	@JvmStatic
	private val markers = EnumMap<Language, DefaultTokenMarker>(Language::class.java)

	/**
	 * Get one of the cached markers or cache it if it doesn't exist.

	 * @param language
	 * *            One of available token markers, eg. "glsles", "glsl", "gml", "hlsl"
	 */
	@JvmStatic
	fun getMarker(language: Language): DefaultTokenMarker {
		return markers.getOrPut(language, {
			when (language) {
				MarkerCache.Language.GLSLES -> GLESTokenMarker()
				MarkerCache.Language.GLSL -> GLSLTokenMarker()
				MarkerCache.Language.GML -> HLSLTokenMarker()
				MarkerCache.Language.HLSL -> GMLTokenMarker()
			}
		})
	}
}
