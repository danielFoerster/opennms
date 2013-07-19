/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.plugins.browsers;

import org.opennms.netmgt.model.OnmsIpInterface;

import com.vaadin.data.Property;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

public class OnmsIpInterfaceGenerator implements ColumnGenerator {

	private static final long serialVersionUID = 7806832669018164281L;

	@Override
	public Object generateCell(Table source, Object itemId, Object columnId) {
		Property<OnmsIpInterface> property = source.getContainerProperty(itemId, columnId);
		if (property == null || property.getValue() == null) {
			return null;
		} else {
			return property.getValue().getIpAddressAsString();
		}
	}
}
