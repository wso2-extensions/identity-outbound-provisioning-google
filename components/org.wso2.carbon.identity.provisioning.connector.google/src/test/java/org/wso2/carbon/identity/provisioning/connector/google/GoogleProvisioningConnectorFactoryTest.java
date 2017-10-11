/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.google;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({LogFactory.class})
public class GoogleProvisioningConnectorFactoryTest {

    private static final String CONNECTOR_TYPE = "googleapps";

    @Mock
    private Log log;

    @DataProvider(name = "provideTestData")
    public Object[][] provideTestData() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    @Test(dataProvider = "provideTestData")
    public void testBuildConnector(boolean debugEnabled) throws Exception {

        mockStatic(LogFactory.class);
        when(LogFactory.getLog(any(Class.class))).thenReturn(log);
        when(log.isDebugEnabled()).thenReturn(debugEnabled);
        doNothing().when(log).debug(any());

        GoogleProvisioningConnectorFactory googleProvisioningConnectorFactory = new
                GoogleProvisioningConnectorFactory();
        Property[] properties = new Property[0];
        AbstractOutboundProvisioningConnector connector = googleProvisioningConnectorFactory.buildConnector(properties);

        Assert.assertTrue(connector instanceof GoogleProvisioningConnector);
    }

    @Test
    public void testGetConnectorType() throws Exception {

        mockStatic(LogFactory.class);
        when(LogFactory.getLog(any(Class.class))).thenReturn(log);

        GoogleProvisioningConnectorFactory googleProvisioningConnectorFactory = new
                GoogleProvisioningConnectorFactory();

        Assert.assertEquals(googleProvisioningConnectorFactory.getConnectorType(), CONNECTOR_TYPE);
    }
}
