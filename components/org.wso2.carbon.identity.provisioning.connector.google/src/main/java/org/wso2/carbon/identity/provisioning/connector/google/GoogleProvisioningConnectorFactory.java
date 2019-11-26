/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.google;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

import java.util.ArrayList;
import java.util.List;

public class GoogleProvisioningConnectorFactory extends AbstractProvisioningConnectorFactory {


    private static final Log log = LogFactory.getLog(GoogleProvisioningConnectorFactory.class);
    private static final String GOOGLE = "googleapps";

    @Override
    protected AbstractOutboundProvisioningConnector buildConnector(
            Property[] provisioningProperties) throws IdentityProvisioningException {
        GoogleProvisioningConnector googleConnector = new GoogleProvisioningConnector();
        googleConnector.init(provisioningProperties);

        if (log.isDebugEnabled()) {
            log.debug("Google provisioning connector created successfully.");
        }

        return googleConnector;
    }


    @Override
    public String getConnectorType() {
        return GOOGLE;
    }

    /**
     * Get Configuration Properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<>();
        Property domain = new Property();
        domain.setName(GoogleConnectorConstants.PropertyConfig.DOMAIN_NAME_KEY);
        domain.setDisplayName("Google Domain");
        domain.setDescription("Name of the Google domain which needed to provision users. Ex: mygoogledomain.com");
        domain.setRequired(true);
        domain.setType("string");
        domain.setDisplayOrder(1);
        configProperties.add(domain);

        Property emailClaim = new Property();
        emailClaim.setName(GoogleConnectorConstants.PropertyConfig.EMAIL_CLAIM_KEY);
        emailClaim.setDisplayName("Primary Email Claim");
        emailClaim.setDescription("How to retrieve primary email address for the account to be created");
        emailClaim.setRequired(true);
        emailClaim.setType("string");
        emailClaim.setDisplayOrder(2);
        configProperties.add(emailClaim);

        Property givenNameClaim = new Property();
        givenNameClaim.setName(GoogleConnectorConstants.PropertyConfig.GIVEN_NAME_CLAIM_KEY);
        givenNameClaim.setDisplayName("Given Name Claim");
        givenNameClaim.setRequired(true);
        givenNameClaim.setDescription("How to retrieve given name attribute for the user");
        givenNameClaim.setType("string");
        givenNameClaim.setDisplayOrder(3);
        configProperties.add(givenNameClaim);

        Property familyNameClaim = new Property();
        familyNameClaim.setName(GoogleConnectorConstants.PropertyConfig.FAMILY_NAME_CLAIM_KEY);
        familyNameClaim.setDisplayName("Family Name Claim");
        familyNameClaim.setRequired(true);
        familyNameClaim.setDescription("How to retrieve family name attribute for the user");
        familyNameClaim.setType("string");
        familyNameClaim.setDisplayOrder(4);
        configProperties.add(familyNameClaim);

        Property serviceAccEmail = new Property();
        serviceAccEmail.setName(GoogleConnectorConstants.PropertyConfig.SERVICE_ACCOUNT_EMAIL_KEY);
        serviceAccEmail.setDisplayName("Service Account Email");
        serviceAccEmail.setRequired(true);
        serviceAccEmail.setDescription("Service account email for authentication. ex: d343s86gf@developer" +
                ".gserviceaccount.com");
        serviceAccEmail.setType("string");
        serviceAccEmail.setDisplayOrder(5);
        configProperties.add(serviceAccEmail);

        Property privateKey = new Property();
        privateKey.setName(GoogleConnectorConstants.PropertyConfig.PRIVATE_KEY);
        privateKey.setDisplayName("Private Key");
        privateKey.setRequired(false);
        privateKey.setDescription("PKCS12 private key generated at the service account creation");
        privateKey.setType("string");
        privateKey.setDisplayOrder(6);
        configProperties.add(privateKey);

        Property adminEmail = new Property();
        adminEmail.setName(GoogleConnectorConstants.PropertyConfig.ADMIN_EMAIL_KEY);
        adminEmail.setDisplayName("Administrator's Email");
        adminEmail.setRequired(true);
        adminEmail.setDescription(
                "Email of the administrator who owns above service account. ex: tom@mygoogledomain.com");
        adminEmail.setType("string");
        adminEmail.setDisplayOrder(7);
        configProperties.add(adminEmail);

        Property appName = new Property();
        appName.setName(GoogleConnectorConstants.PropertyConfig.APPLICATION_NAME_KEY);
        appName.setDisplayName("Application Name");
        appName.setRequired(true);
        appName.setDescription("Application name to represent this connector");
        appName.setType("string");
        appName.setDisplayOrder(8);
        configProperties.add(appName);

        Property provPattern = new Property();
        provPattern.setName(GoogleConnectorConstants.PropertyConfig.PROVISIONING_PATTERN_KEY);
        provPattern.setDisplayName("Google Outbound Provisioning pattern");
        provPattern.setRequired(false);
        provPattern.setDescription("This pattern is used to build the user id of google domain. Combination of " +
                "attributes UD (User Domain), UN (Username), TD (Tenant Domain) and IDP (Identity Provider) can be " +
                "used to construct a valid pattern. Ex: {UD, UN, TD, IDP}");
        provPattern.setType("string");
        provPattern.setDisplayOrder(9);
        configProperties.add(provPattern);

        Property provSeparator = new Property();
        provSeparator.setName(GoogleConnectorConstants.PropertyConfig.PROVISIONING_SEPERATOR_KEY);
        provSeparator.setDisplayName("Google Provisioning Separator");
        provSeparator.setRequired(false);
        provSeparator.setDescription("This is the separator of attributes in Google Outbound Provisioning pattern." +
                "For example if pattern is {UN,TD} and Username: testUser, Tenant Domain: TestTenant.com, " +
                "Separator:_, Google Domain : testmail.com then the privisioining email is testUser_testTenant" +
                ".com@testmail.com");
        provSeparator.setType("string");
        provSeparator.setDisplayOrder(10);
        configProperties.add(provSeparator);

        return configProperties;
    }
}
