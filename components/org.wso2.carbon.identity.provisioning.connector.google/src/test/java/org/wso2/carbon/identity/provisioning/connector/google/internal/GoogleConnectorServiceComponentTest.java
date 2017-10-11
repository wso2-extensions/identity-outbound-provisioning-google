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

package org.wso2.carbon.identity.provisioning.connector.google.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Dictionary;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({LogFactory.class})
public class GoogleConnectorServiceComponentTest {

    @Mock
    private ComponentContext componentContext;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Log log;

    @DataProvider(name = "provideTestData")
    public Object[][] provideTestData() {
        return new Object[][]{
                {true, false},
                {true, true},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "provideTestData")
    public void testActivate(boolean debugEnabled, boolean throwError) throws Exception {

        mockStatic(LogFactory.class);
        when(LogFactory.getLog(any(Class.class))).thenReturn(log);
        when(log.isDebugEnabled()).thenReturn(debugEnabled);
        doNothing().when(log).debug(any());
        Mockito.reset(componentContext);

        if (throwError) {
            when(componentContext.getBundleContext()).thenThrow(new RuntimeException("Test Exception."));
        } else {
            when(componentContext.getBundleContext()).thenReturn(bundleContext);
            when(bundleContext.registerService(anyString(), any(), any(Dictionary.class))).thenReturn(null);
        }

        GoogleConnectorServiceComponent component = new GoogleConnectorServiceComponent();
        component.activate(componentContext);
        Assert.assertTrue(true);
    }
}
