/**
 * @file MarkerCache.java
 * @brief Class implementing token marker caching for efficiency.
 * @section License
 * <p>
 * Copyright (C) 2013 Robert B. Colton
 * This file is a part of the LateralGM IDE.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.lateralgm.components;

import org.lateralgm.joshedit.DefaultTokenMarker;
import org.lateralgm.joshedit.lexers.GLESTokenMarker;
import org.lateralgm.joshedit.lexers.GLSLTokenMarker;
import org.lateralgm.joshedit.lexers.GMLTokenMarker;
import org.lateralgm.joshedit.lexers.HLSLTokenMarker;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A non-instantiable token marker cache that avoids instantiating more
 * markers than is necessary.
 *
 * @author Robert B. Colton
 */
public final class MarkerCache {
	public enum Language {
		GLSLES, GLSL, GML, HLSL
	}
	/** The cached token markers keyed with the language **/
	private static EnumMap<Language, DefaultTokenMarker> markers = new EnumMap<>(Language.class);

	/**
	 * Get one of the cached markers or cache it if it doesn't exist.
	 *
	 * @param language
	 *            One of available token markers, eg. "glsles", "glsl", "gml", "hlsl"
	 **/
	public static DefaultTokenMarker getMarker(Language language) {
		DefaultTokenMarker marker = markers.get(language);
		if (marker == null) {
			switch (language) {
				case GLSLES:
					marker = new GLESTokenMarker();
					break;
				case GLSL:
					marker = new GLSLTokenMarker();
					break;
				case GML:
					marker = new HLSLTokenMarker();
					break;
				case HLSL:
					marker = new GMLTokenMarker();
					break;
			}
			markers.put(language, marker);
		}
		return marker;
	}

}
