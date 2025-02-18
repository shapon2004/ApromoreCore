/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
/*
 * OpenXES
 * 
 * The reference implementation of the XES meta-model for event 
 * log data management.
 * 
 * Copyright (c) 2009 Christian W. Guenther (christian@deckfour.org)
 * 
 * 
 * LICENSE:
 * 
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * EXEMPTION:
 * 
 * The use of this software can also be conditionally licensed for
 * other programs, which do not satisfy the specified conditions. This
 * requires an exemption from the general license, which may be
 * granted on a per-case basis.
 * 
 * If you want to license the use of this software with a program
 * incompatible with the LGPL, please contact the author for an
 * exemption at the following email address: 
 * christian@deckfour.org
 * 
 */
package org.deckfour.xes.out;

import org.deckfour.xes.util.XRegistry;

/**
 * System-wide registry for XES serializer implementations.
 * Applications can use this registry as a convenience
 * to provide an overview about serializeable formats, e.g.,
 * in the user interface.
 * Any custom serializer implementation can be registered
 * with this registry, so that it transparently becomes
 * available also to any other using application.
 * 
 * @author Christian W. Guenther (christian@deckfour.org)
 */
public class XSerializerRegistry extends XRegistry<XSerializer> {
	
	/**
	 * Singleton registry instance.
	 */
	private static XSerializerRegistry singleton = new XSerializerRegistry();
	
	/**
	 * Retrieves the singleton registry instance.
	 */
	public static XSerializerRegistry instance() {
		return singleton;
	}
	
	/**
	 * Creates the singleton.
	 */
	private XSerializerRegistry() {
		super();
		register(new XMxmlSerializer());
		register(new XMxmlGZIPSerializer());
		register(new XesXmlSerializer());
		setCurrentDefault(new XesXmlGZIPSerializer());
	}

	/* (non-Javadoc)
	 * @see org.deckfour.xes.util.XRegistry#areEqual(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected boolean areEqual(XSerializer a, XSerializer b) {
		return a.getClass().equals(b.getClass());
	}

}
