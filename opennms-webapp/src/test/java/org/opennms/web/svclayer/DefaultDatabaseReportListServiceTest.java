/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
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

package org.opennms.web.svclayer;

import org.junit.Before;
import org.junit.Test;
import org.opennms.web.svclayer.support.DatabaseReportDescription;

import java.util.List;

import static org.junit.Assert.assertEquals;

//import org.opennms.netmgt.dao.castor.DefaultDatabaseReportConfigDao;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
public class DefaultDatabaseReportListServiceTest {

    private DatabaseReportListService m_descriptionService;
    
    @Before
    public void setupDao() throws Exception {
        System.setProperty("opennms.home", "src/test/resources");
        
//        m_dao = new DefaultDatabaseReportConfigDao();
//        Resource resource = new ClassPathResource("/database-reports-testdata.xml");
//        m_dao.setConfigResource(resource);
//        m_dao.afterPropertiesSet();
//        m_descriptionService = new DefaultDatabaseReportListService();
//        m_descriptionService.setDatabaseReportConfigDao(m_dao);
//        m_descriptionService = new DefaultDatabaseReportListService();
//        m_descriptionService.setDatabaseReportConfigDao(m_dao);
    }

    @Test
    public void testGetAll() throws Exception {
        //List<DatabaseReportDescription> description = m_descriptionService.getAll();
        System.out.println("Huhu: " + m_descriptionService);
        //assertEquals(2,description.size());
    }
    
    @Test
    public void testGetAlOnlinel() throws Exception {
        List<DatabaseReportDescription> description = m_descriptionService.getAllOnline();
        assertEquals(1,description.size());
    }

    public void setDatabaseReportListService(DatabaseReportListService databaseReportListService) {
        m_descriptionService = databaseReportListService;
    }
}
