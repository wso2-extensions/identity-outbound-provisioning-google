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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GoogleProvisioningConnectorTest {

    private static final int CASE_1 = 1;
    private static final int CASE_2 = 2;
    private static final int CASE_3 = 3;
    private static final int CASE_4 = 4;
    private String username = "testuser";

    @Mock
    private Log log;

    @BeforeClass
    public void setUpClass() {
        // CarbonContext's static initializer calls CarbonUtils.getCarbonHome() which tries to set
        // the "carbon.home" system property. Without it being set, the initializer throws a
        // NullPointerException, which prevents Mockito from instrumenting PrivilegedCarbonContext.
        if (System.getProperty("carbon.home") == null) {
            System.setProperty("carbon.home", System.getProperty("java.io.tmpdir"));
        }
    }

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInit() throws Exception {
        String privateKey = "googlePrvKey";

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            logFactoryMock.when(() -> LogFactory.getLog(any(Class.class))).thenReturn(log);
            when(log.isDebugEnabled()).thenReturn(true);
            doNothing().when(log).debug(any());

            GoogleProvisioningConnector connector = new GoogleProvisioningConnector();

            Property property1 = new Property();
            property1.setName(GoogleConnectorConstants.PRIVATE_KEY);
            property1.setValue(privateKey);
            Property property2 = new Property();
            property2.setName(IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED);
            property2.setValue("1");
            Property[] provisioningProperties = new Property[]{property1, property2};

            connector.init(provisioningProperties);
            Path filePath = Paths.get(privateKey);
            boolean fileExist = Files.exists(filePath);

            Assert.assertTrue(fileExist, privateKey + " file is not created.");

            Files.delete(filePath);
        }
    }

    @Test
    public void testGetClaimDialectUri() throws Exception {

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, false);

            GoogleProvisioningConnector connector = new GoogleProvisioningConnector();
            Assert.assertNull(connector.getClaimDialectUri());
        }
    }

    @DataProvider(name = "provisionDataProvider")
    public Object[][] provideTestData() {

        ProvisioningEntity entity1 = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.DELETE);
        ProvisioningEntity entity2 = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST);
        ProvisioningEntity entity3 = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.PUT);
        ProvisioningEntity entity4 = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.PATCH);
        ProvisioningEntity entity5 = new ProvisioningEntity(ProvisioningEntityType.GROUP, ProvisioningOperation.DELETE);

        return new Object[][]{
                {true, "0", entity1},
                {true, "1", entity1},
                {true, "1", entity2},
                {true, "1", entity3},
                {true, "1", null},
                {false, "0", entity1},
                {false, "0", entity2},
                {false, "0", entity3},
                {false, "0", null},
                {false, "1", entity1},
                {false, "1", entity2},
                {false, "1", entity3},
                {true, "1", null},
                {false, "1", entity4},
                {false, "1", entity5}
        };
    }

    @Test(dataProvider = "provisionDataProvider")
    public void testProvision(boolean jitProvision, String jitEnable, Object entityObj) throws Exception {

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, false);

            Property property1 = new Property();
            property1.setName(IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED);
            property1.setValue(jitEnable);
            Property[] provisioningProperties = new Property[]{property1};

            GoogleProvisioningConnector connector = spy(getConnector(provisioningProperties));
            ProvisioningEntity entity = (ProvisioningEntity) entityObj;
            String provisionedId = "test-provisioned-id";

            if (entity != null) {
                entity.setJitProvisioning(jitProvision);

                if (ProvisioningOperation.DELETE.equals(entity.getOperation())) {
                    doNothing().when(connector).deleteUser(any(ProvisioningEntity.class));
                } else if (ProvisioningOperation.POST.equals(entity.getOperation())) {
                    doReturn(provisionedId).when(connector).createUser(any(ProvisioningEntity.class));
                } else if (ProvisioningOperation.PUT.equals(entity.getOperation())) {
                    doNothing().when(connector).updateUser(any(ProvisioningEntity.class));
                }
            }

            ProvisionedIdentifier identifier = connector.provision(entity);

            if (jitProvision && "0".equals(jitEnable)) {
                Assert.assertNull(identifier);
            } else if (entity == null) {
                Assert.assertNull(identifier);
            } else if (ProvisioningOperation.PUT.equals(entity.getOperation())) {
                Assert.assertNull(identifier);
            } else if (ProvisioningOperation.PATCH.equals(entity.getOperation())) {
                Assert.assertNull(identifier);
            } else if (!ProvisioningEntityType.USER.equals(entity.getEntityType())) {
                Assert.assertNull(identifier);
            } else {
                Assert.assertNotNull(identifier);
            }
        }
    }

    @DataProvider(name = "googleUserDataProvider")
    public Object[][] provideGoogleUserTestData() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    @Test(dataProvider = "googleUserDataProvider")
    public void testBuildGoogleUser(boolean debugEnabled) throws Exception {

        // Use {UN} pattern so buildUserId returns the entity name (without domain).
        // identityProviderName must be non-null to avoid NPE inside buildUserId.
        String claimUri = "org:wso2:carbon:identity:provisioning:claim:username";
        String provisioningPatternKey = "google_prov_pattern";
        String entityName = "usernetityname";

        ClaimMapping claimMapping = new ClaimMapping();
        Claim claim = new Claim();
        claim.setClaimUri(claimUri);
        claimMapping.setLocalClaim(claim);

        Map<ClaimMapping, List<String>> attributes = new HashMap<>();
        attributes.put(claimMapping, Collections.singletonList(username));

        ProvisioningEntity entity = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST,
                                                           attributes);
        entity.setEntityName(entityName);

        Property property1 = buildProperty(provisioningPatternKey, "{UN}");
        Property property2 = buildProperty("identityProviderName", "testIdP");
        Property[] provisioningProperties = new Property[]{property1, property2};

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class);
             MockedStatic<PrivilegedCarbonContext> carbonContextMock = mockStatic(PrivilegedCarbonContext.class)) {
            setLogging(logFactoryMock, debugEnabled);

            PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
            carbonContextMock.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
            when(carbonContext.getTenantDomain()).thenReturn("carbon.super");

            GoogleProvisioningConnector connector = getConnector(provisioningProperties);
            User user = connector.buildGoogleUser(entity);

            Assert.assertEquals(user.getPrimaryEmail(), entityName);
            Assert.assertEquals(user.getName().getGivenName(), username);
            Assert.assertEquals(user.getName().getFamilyName(), username);
            Assert.assertNotNull(user.getPassword(), "Password cannot be null.");
        }
    }

    @DataProvider(name = "directoryServiceDataProvider")
    public Object[][] provideDirectoryServiceTestData() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    @Test(dataProvider = "directoryServiceDataProvider")
    public void testGetDirectoryService(boolean debugEnabled) throws Exception {

        String serviceAccountEmailKey = "google_prov_service_acc_email";
        String adminEmailKey = "google_prov_admin_email";
        String applicationNameKey = "google_prov_application_name";

        Property property1 = buildProperty(serviceAccountEmailKey, "service@gmail.com");
        Property property2 = buildProperty(adminEmailKey, "admin@gmail.com");
        Property property3 = buildProperty(applicationNameKey, "testApp");
        Property[] provisioningProperties = new Property[]{property1, property2, property3};

        GoogleProvisioningConnector connector = getConnector(provisioningProperties);

        Path path = Paths.get("src", "test", "resources", "googlePrvKey");
        Field googlePrvKeyField = GoogleProvisioningConnector.class.getDeclaredField("googlePrvKey");
        googlePrvKeyField.setAccessible(true);
        googlePrvKeyField.set(null, path.toFile());

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            setLogging(logFactoryMock, debugEnabled);

            // mockStatic mocks ALL SecurityUtils methods, so getPkcs12KeyStore() returns null by
            // default. Stub it explicitly so the non-null KeyStore is passed on to
            // loadPrivateKeyFromKeyStore, making the any(KeyStore.class) matcher work.
            KeyStore mockKeyStore = mock(KeyStore.class);
            securityUtilsMock.when(SecurityUtils::getPkcs12KeyStore).thenReturn(mockKeyStore);

            PrivateKey privateKey = mock(PrivateKey.class);
            securityUtilsMock.when(() -> SecurityUtils.loadPrivateKeyFromKeyStore(
                    any(KeyStore.class), any(InputStream.class), anyString(), anyString(), anyString()))
                    .thenReturn(privateKey);

            Directory directoryService = connector.getDirectoryService();
            Assert.assertNotNull(directoryService, "Directory Service cannot be null.");
        }
    }

    @DataProvider(name = "updateGoogleUserDataProvider")
    public Object[][] provideUpdateGoogleUserTestData() {

        String claimUri = "org:wso2:carbon:identity:provisioning:claim:custom claim";
        ClaimMapping claimMapping = new ClaimMapping();
        Claim claim = new Claim();
        claim.setClaimUri(claimUri);
        claimMapping.setRemoteClaim(claim);

        Map<ClaimMapping, List<String>> attributes1 = new HashMap<>();
        attributes1.put(claimMapping, Collections.singletonList(username));
        Map<ClaimMapping, List<String>> attributes2 = new HashMap<>();

        return new Object[][]{
                {true, attributes1},
                {false, attributes1},
                {false, attributes2}
        };
    }

    @Test(dataProvider = "updateGoogleUserDataProvider")
    public void testUpdateGoogleUser(boolean debugEnabled, Object attributesObj) throws Exception {

        String defaultFamilyNameKey = "google_prov_familyname";
        String defaultGivenNameKey = "google_prov_givenname";
        String defaultFamilyName = "defaultFamilyName";
        String defaultGivenName = "defaultGivenName";
        String email = "testuser@gmail.com";
        Map<ClaimMapping, List<String>> attributes = (Map<ClaimMapping, List<String>>) attributesObj;

        ProvisioningEntity entity = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST,
                                                           attributes);
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(email);
        entity.setIdentifier(identifier);

        Property property1 = buildProperty(defaultFamilyNameKey, defaultFamilyName);
        Property property2 = buildProperty(defaultGivenNameKey, defaultGivenName);
        Property[] provisioningProperties = new Property[]{property1, property2};

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, debugEnabled);

            GoogleProvisioningConnector connector = getConnector(provisioningProperties);
            User user = connector.updateGoogleUser(entity);

            if (!attributes.isEmpty()) {
                Assert.assertNotNull(user);
                Assert.assertEquals(user.getName().getFamilyName(), defaultFamilyName);
                Assert.assertEquals(user.getName().getGivenName(), defaultGivenName);
            } else {
                Assert.assertNull(user);
            }
        }
    }

    @DataProvider(name = "updateUserDataProvider")
    public Object[][] provideUpdateUserTestData() {

        String email = "testuser@gmail.com";
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(email);

        return new Object[][]{
                // CASE_1: ProvisioningEntity is null.
                {false, identifier, CASE_1},
                {true, identifier, CASE_1},
                // CASE_2: ProvisionedIdentifier is null.
                {true, null, CASE_2},
                // CASE_3: Happy path.
                {false, identifier, CASE_3},
                {true, identifier, CASE_3},
                // CASE_4: user update throws an IOException.
                {false, identifier, CASE_4},
        };
    }

    @Test(dataProvider = "updateUserDataProvider")
    public void testUpdateUser(boolean debugEnabled, Object identifierObj, int caseNo) throws Exception {

        ProvisionedIdentifier identifier = (ProvisionedIdentifier) identifierObj;
        Property[] provisioningProperties = new Property[]{};

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, debugEnabled);

            GoogleProvisioningConnector connector = spy(getConnector(provisioningProperties));
            ProvisioningEntity entity = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST);
            entity.setIdentifier(identifier);

            User user = mock(User.class);
            if (caseNo == CASE_1) {
                doReturn(null).when(connector).updateGoogleUser(any(ProvisioningEntity.class));
            } else {
                doReturn(user).when(connector).updateGoogleUser(any(ProvisioningEntity.class));
            }

            Directory directory = mock(Directory.class);
            doReturn(directory).when(connector).getDirectoryService();
            Directory.Users users = mock(Directory.Users.class);
            when(directory.users()).thenReturn(users);
            Directory.Users.Update updateRequest = mock(Directory.Users.Update.class);
            when(users.update(anyString(), any(User.class))).thenReturn(updateRequest);

            if (caseNo == CASE_4) {
                when(updateRequest.execute()).thenThrow(new IOException("Test IO Exception."));
            }
            try {
                connector.updateUser(entity);
                if (caseNo == CASE_2 || caseNo == CASE_4) {
                    Assert.fail("Test expects an IdentityProvisioningException for updateUser().");
                }
            } catch (IdentityProvisioningException e) {
                if (caseNo == CASE_2 || caseNo == CASE_4) {
                    Assert.assertTrue(true, "Test expects an IdentityProvisioningException for updateUser().");
                } else {
                    Assert.fail("updateUser() should not throw an error.");
                }
            }
        }
    }

    @DataProvider(name = "createUserDataProvider")
    public Object[][] provideCreateUserTestData() {

        String email = "testuser@gmail.com";
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(email);

        return new Object[][]{
                // CASE_1: Happy path.
                {true, CASE_1},
                // CASE_2: user create throws an IOException.
                {false, CASE_2},
        };
    }

    @Test(dataProvider = "createUserDataProvider")
    public void testCreateUser(boolean debugEnabled, int caseNo) throws Exception {
        String email = "testuser@gmail.com";

        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        Property[] provisioningProperties = new Property[]{};

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, debugEnabled);

            GoogleProvisioningConnector connector = spy(getConnector(provisioningProperties));
            ProvisioningEntity entity = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST);
            entity.setEntityName(email);
            entity.setIdentifier(identifier);

            User user = mock(User.class);
            doReturn(user).when(connector).buildGoogleUser(any(ProvisioningEntity.class));
            Directory directory = mock(Directory.class);
            doReturn(directory).when(connector).getDirectoryService();
            Directory.Users users = mock(Directory.Users.class);
            when(directory.users()).thenReturn(users);
            Directory.Users.Insert insertRequest = mock(Directory.Users.Insert.class);
            when(users.insert(any(User.class))).thenReturn(insertRequest);

            if (caseNo == CASE_1) {
                when(insertRequest.execute()).thenReturn(user);
            } else if (caseNo == CASE_2) {
                when(insertRequest.execute()).thenThrow(new IOException("Test IOException."));
            }
            when(user.getPrimaryEmail()).thenReturn(email);

            String createdUser;
            try {
                createdUser = connector.createUser(entity);
                if (caseNo == CASE_1) {
                    Assert.assertNotNull(createdUser, "createdUser cannot be null.");
                    Assert.assertEquals(createdUser, email);
                } else if (caseNo == CASE_2) {
                    Assert.fail("Test expects an IdentityProvisioningException for createUser().");
                }
            } catch (IdentityProvisioningException e) {
                if (caseNo == CASE_2) {
                    Assert.assertTrue(true, "Test expects an IdentityProvisioningException for deleteUser().");
                } else {
                    Assert.fail("createUser() should not throw an error.");
                }
            }
        }
    }

    @DataProvider(name = "deleteUserDataProvider")
    public Object[][] provideDeleteUserTestData() {

        String email = "testuser@gmail.com";
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(email);

        return new Object[][]{
                // CASE_1: Happy path.
                {true, identifier, CASE_1},
                {false, identifier, CASE_1},
                // CASE_2: ProvisionedIdentifier is null.
                {true, null, CASE_2},
                // CASE_3: user delete throws an IOException
                {true, identifier, CASE_3},
                // CASE_4: user delete throws a GoogleJsonResponseException
                {true, identifier, CASE_4},
                {false, identifier, CASE_4},
        };
    }

    @Test(dataProvider = "deleteUserDataProvider")
    public void testDeleteUser(boolean debugEnabled, Object identifierObj, int caseNo) throws Exception {
        ProvisionedIdentifier identifier = (ProvisionedIdentifier) identifierObj;
        Property[] provisioningProperties = new Property[]{};

        try (MockedStatic<LogFactory> logFactoryMock = mockStatic(LogFactory.class)) {
            setLogging(logFactoryMock, debugEnabled);

            GoogleProvisioningConnector connector = spy(getConnector(provisioningProperties));
            ProvisioningEntity entity = new ProvisioningEntity(ProvisioningEntityType.USER, ProvisioningOperation.POST);
            entity.setIdentifier(identifier);

            Directory directory = mock(Directory.class);
            doReturn(directory).when(connector).getDirectoryService();
            Directory.Users users = mock(Directory.Users.class);
            when(directory.users()).thenReturn(users);
            Directory.Users.Delete deleteRequest = mock(Directory.Users.Delete.class);
            when(users.delete(anyString())).thenReturn(deleteRequest);

            if (caseNo == CASE_3) {
                when(deleteRequest.execute()).thenThrow(new IOException("Test IO Exception."));
            } else if (caseNo == CASE_4) {
                GoogleJsonResponseException exp = mock(GoogleJsonResponseException.class);
                int errorCode = 404;
                when(exp.getStatusCode()).thenReturn(errorCode);
                when(deleteRequest.execute()).thenThrow(exp);
            }

            try {
                connector.deleteUser(entity);
                if (caseNo == CASE_2 || caseNo == CASE_3) {
                    Assert.fail("Test expects an IdentityProvisioningException for deleteUser().");
                }
            } catch (IdentityProvisioningException e) {
                if (caseNo == CASE_2 || caseNo == CASE_3) {
                    Assert.assertTrue(true, "Test expects an IdentityProvisioningException for deleteUser().");
                } else {
                    Assert.fail("deleteUser() should not throw an error.");
                }
            }
        }
    }

    private GoogleProvisioningConnector getConnector(Property[] properties) throws IdentityProvisioningException {

        GoogleProvisioningConnector connector = new GoogleProvisioningConnector();
        connector.init(properties);
        return connector;
    }

    private Property buildProperty(String name, String value) {

        Property property = new Property();
        property.setName(name);
        property.setValue(value);
        return property;
    }

    private void setLogging(MockedStatic<LogFactory> logFactoryMock, boolean debugEnabled) {

        logFactoryMock.when(() -> LogFactory.getLog(any(Class.class))).thenReturn(log);
        doNothing().when(log).warn(ArgumentMatchers.any());
        when(log.isDebugEnabled()).thenReturn(debugEnabled);
        doNothing().when(log).debug(any());
    }
}
