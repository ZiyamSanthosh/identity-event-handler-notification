/*
 * Copyright (c) 2019-2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.email.mgt;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.NOTIFICATION_TEMPLATES_STORAGE_CONFIG;

import org.apache.commons.lang.StringUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Class that contains the test cases for the implementation of Email Template Manager.
 */
@PrepareForTest({IdentityValidationUtil.class, I18nMgtDataHolder.class, CarbonUtils.class,
        OrganizationManagementUtil.class, IdentityUtil.class})
public class ApplicationEmailTemplateTest extends PowerMockTestCase {

    private EmailTemplateManagerImpl emailTemplateManager;

    @Mock
    RegistryResourceMgtService resourceMgtService;

    @Mock
    I18nMgtDataHolder i18nMgtDataHolder;

    @Mock
    Resource resource;

    @Mock
    ApplicationManagementService applicationManagementService;

    @Mock
    OrganizationManager organizationManager;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {

        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    private String tenantDomain = "carbon.super";

    @BeforeMethod
    public void setUp() {

        initMocks(this);
        mockStatic(I18nMgtDataHolder.class);
        i18nMgtDataHolder = PowerMockito.mock(I18nMgtDataHolder.class);
        when(I18nMgtDataHolder.getInstance()).thenReturn(i18nMgtDataHolder);

        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty(NOTIFICATION_TEMPLATES_STORAGE_CONFIG)).thenReturn("registry");

        // Mock RegistryResourceMgtService.
        when(i18nMgtDataHolder.getRegistryResourceMgtService()).thenReturn(resourceMgtService);
        when(i18nMgtDataHolder.getApplicationManagementService()).thenReturn(applicationManagementService);
        when(i18nMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
        emailTemplateManager = new EmailTemplateManagerImpl();

        mockStatic(OrganizationManagementUtil.class);
    }

    /**
     * Contains the test scenarios for getting notification template using the notification channel.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Display name
     * @param type                Type
     * @param locale              Locale
     * @param contentType         Content type
     * @param content             Template content
     * @throws Exception Error testing getNotificationTemplate implementation.
     */
    @Test(dataProvider = "notificationTemplateDataProvider")
    public void testGetNotificationTemplate(String notificationChannel, String displayName, String type, String locale,
            String contentType, byte[] content, String applicationUuid) throws Exception {

        mockRegistryResource(notificationChannel, displayName, type, locale, contentType, content, null, null);
        mockIsValidTemplate(true, true);
        NotificationTemplate notificationTemplate = emailTemplateManager
                .getNotificationTemplate(notificationChannel, type, locale, tenantDomain, applicationUuid);
        validateNotificationTemplate(notificationTemplate, notificationChannel);
    }

    /**
     * Contains the test scenarios for getting notification template for shared applications.
     *
     * @param notificationChannel Notification channel of the template.
     * @param displayName         Display name of the template.
     * @param type                Template type.
     * @param locale              Locale of the notification template.
     * @param contentType         Content type of the template.
     * @param content             Template content.
     * @param applicationUuid     The UUID of the application from which the template should be retrieved.
     * @throws Exception Errors occurred while testing getNotificationTemplate implementation.
     */
    @Test(dataProvider = "notificationTemplateDataProvider")
    public void testGetNotificationTemplateForSharedApps(String notificationChannel, String displayName, String type,
                                                         String locale, String contentType, byte[] content,
                                                         String applicationUuid) throws Exception {

        String rootApplicationId = "root-application-id";
        String subOrganizationId = "sub-organization-id";
        String primaryOrganizationId = "primary-organization-id";
        mockRegistryResource(notificationChannel, displayName, type, locale, contentType, content, tenantDomain,
                rootApplicationId);
        when(OrganizationManagementUtil.isOrganization(ArgumentMatchers.eq(subOrganizationId))).thenReturn(true);
        when(applicationManagementService.getMainAppId(ArgumentMatchers.eq(applicationUuid))).thenReturn(
                rootApplicationId);
        when(organizationManager.resolveOrganizationId(ArgumentMatchers.eq(subOrganizationId))).thenReturn(subOrganizationId);
        when(organizationManager.getPrimaryOrganizationId(ArgumentMatchers.eq(subOrganizationId))).thenReturn(primaryOrganizationId);
        when(organizationManager.resolveTenantDomain(ArgumentMatchers.eq(primaryOrganizationId))).thenReturn(tenantDomain);
        NotificationTemplate notificationTemplate =
                emailTemplateManager.getNotificationTemplate(notificationChannel, type, locale, subOrganizationId,
                        applicationUuid);
        validateNotificationTemplate(notificationTemplate, notificationChannel);
    }

    /**
     * Contains the error scenarios for resolving notification template.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Display name
     * @param type                Type
     * @param locale              Locale
     * @param content             Template content
     * @param isValidTemplate     Is valid template
     * @param isValidLocale       Is valid locale
     * @param errorMsg            Error message
     * @param expectedErrorCode   Expected error code
     * @param contentType         Content type
     * @throws Exception Error testing getNotificationTemplate implementation.
     */
    @Test(dataProvider = "invalidNotificationTemplateDataProvider")
    public void testGetNotificationTemplateErrors(String notificationChannel, String displayName, String type,
            String locale, String contentType, boolean isValidTemplate, boolean isValidLocale, String errorMsg,
            String expectedErrorCode, byte[] content, String applicationUuid) throws Exception {

        when(OrganizationManagementUtil.isOrganization(tenantDomain)).thenReturn(false);
        mockIsValidTemplate(isValidTemplate, isValidLocale);
        try {
            mockRegistryResource(notificationChannel, displayName, type, locale, contentType, content, null, null);
            NotificationTemplate notificationTemplate = emailTemplateManager
                    .getNotificationTemplate(notificationChannel, type, locale, tenantDomain, applicationUuid);
            assertNull(notificationTemplate, "Cannot return a notificationTemplate");
        } catch (NotificationTemplateManagerException e) {
            String errorCode = e.getErrorCode();
            assertNotNull(errorCode, "Error code cannot be empty");
            if (StringUtils.isEmpty(errorMsg)) {
                errorMsg = e.getMessage();
            }
            assertEquals(errorCode, expectedErrorCode, errorMsg);
        }
    }

    /**
     * Test error scenarios of adding a notification template type.
     *
     * @param templateName Notification template name
     * @param channel      Notification channel
     * @param domain       Tenant domain
     * @param errorCode    Expected error code (NOTE: Without the scenario code)
     * @param errorMessage Error message
     * @param scenarioCode Error scenario
     */
    @Test(dataProvider = "addNotificationTemplateTypeProvider")
    public void TestAddNotificationTemplateType(String templateName, String channel, String domain, String errorCode,
                                                String errorMessage, int scenarioCode, String applicationUuid) {

        try {
            if (scenarioCode == 2) {
                when(resourceMgtService.isResourceExists(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
            }
            if (scenarioCode == 3) {
                when(resourceMgtService.isResourceExists(Matchers.anyString(), Matchers.anyString()))
                        .thenThrow(new IdentityRuntimeException("Test Error"));
            }
            emailTemplateManager
                    .addNotificationTemplateType(templateName, channel, domain, applicationUuid);
        } catch (NotificationTemplateManagerException e) {
            String expectedCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(errorCode,
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            assertEquals(e.getErrorCode(), expectedCode, errorMessage);
        }
    }

    /**
     * Test the error scenarios of AddNotificationTemplate method.
     *
     * @param tenantDomain    Tenant domain
     * @param errorCode       Error code
     * @param errorMessage    Error message
     * @param templateContent Contents to build notification template
     * @throws Exception Error in the test scenario
     */
    @Test(dataProvider = "addNotificationTemplateProvider")
    public void testAddNotificationTemplate(String tenantDomain, String errorCode,
                                            String errorMessage, String[] templateContent, String applicationUuid)
            throws Exception {

        NotificationTemplate notificationTemplate;
        if (templateContent == null) {
            notificationTemplate = null;
        } else {
            notificationTemplate = buildSampleNotificationTemplate(templateContent);
        }
        try {
            when(resourceMgtService.isResourceExists(Matchers.anyString(), Matchers.anyString()))
                    .thenThrow(new IdentityRuntimeException("Test Error"));
            emailTemplateManager.addNotificationTemplate(notificationTemplate, tenantDomain, applicationUuid);
            throw new Exception("Exception should be thrown for above testing scenarios");
        } catch (NotificationTemplateManagerException e) {
            if (StringUtils.isBlank(e.getErrorCode())) {
                throw new Exception("Error code cannot be NULL", e);
            }
            String expectedCode = I18nEmailUtil.prependOperationScenarioToErrorCode(errorCode,
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            assertEquals(e.getErrorCode(), expectedCode, errorMessage);
        }
    }

    /**
     * Contains notification templates and error scenarios for addNotificationTemplate API.
     *
     * @return Object[][]
     */
    @DataProvider(name = "addNotificationTemplateProvider")
    private Object[][] addNotificationTemplateProvider() {

        String tenantDomain = "test domain";
        String applicationUuid = "test-uuid";
        String displayName = "Test Value";
        String testNotificationChannel = "Test Value";
        String type = "Test Value";
        String contentType = "Test Value";
        String locale = "Test Value";
        String body = "Test Value";
        String subject = "Test Value";
        String footer = "Test Value";
        String smsChannel = NotificationChannels.SMS_CHANNEL.getChannelType();
        String emailChannel = NotificationChannels.EMAIL_CHANNEL.getChannelType();

        String errorCode1 = I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getCode();
        String message1 = "Empty NotificationTemplate object :";

        String errorCode2 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode();
        String message2 = "Empty template name in the notification template object : ";
        String[] templateContent2 =
                {StringUtils.EMPTY, testNotificationChannel, type, contentType, locale, body, subject, footer};

        String errorCode3 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getCode();
        String message3 = "Empty locale in the notification template object : ";
        String[] templateContent3 =
                {displayName, testNotificationChannel, type, contentType, StringUtils.EMPTY, body, subject, footer};

        String errorCode4 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getCode();
        String message4 = "Invalid SMS template : ";
        String[] templateContent4 =
                {displayName, smsChannel, type, contentType, locale, StringUtils.EMPTY, subject, footer};

        String errorCode5 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getCode();
        String message5 = "Empty notification channel in the notification template object : ";
        String[] templateContent5 = {displayName, StringUtils.EMPTY, type, contentType, locale, body, subject, footer};

        String errorCode6 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode();
        String message6 = "Invalid content in the SMS template : ";
        String[] templateContent6 = {displayName, smsChannel, type, contentType, locale, body, subject, footer};

        String errorCode7 = I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getCode();
        String message7 = "Invalid EMAIL template : ";
        String[] templateContent7 =
                {displayName, emailChannel, type, contentType, locale, body, StringUtils.EMPTY, footer};

        String errorCode8 = I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode();
        String message8 = "Invalid EMAIL template : ";
        String[] templateContent8 =
                {displayName, emailChannel, type, contentType, locale, body, subject, footer};

        return new Object[][]{
                {tenantDomain, errorCode1, message1, null, applicationUuid},
                {tenantDomain, errorCode2, message2, templateContent2, applicationUuid},
                {tenantDomain, errorCode3, message3, templateContent3, applicationUuid},
                {tenantDomain, errorCode4, message4, templateContent4, applicationUuid},
                {tenantDomain, errorCode5, message5, templateContent5, applicationUuid},
                {tenantDomain, errorCode6, message6, templateContent6, applicationUuid},
                {tenantDomain, errorCode7, message7, templateContent7, applicationUuid},
                {tenantDomain, errorCode8, message8, templateContent8, applicationUuid}
        };
    }

    /**
     * Contains the details of error codes and error scenarios.
     *
     * @return Object[][]
     */
    @DataProvider(name = "addNotificationTemplateTypeProvider")
    private Object[][] addNotificationTemplateTypeProvider() {

        String testTemplateName = "Test template";
        String testChannel = "Test Channel";
        String testDomain = "Test Domain";
        String applicationUuid = "test-uuid";

        int testScenario1 = 1;
        String expectedErrorCode1 = I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode();
        String errorMessage1 = "TEST EMPTY notification template template name : ";

        int testScenario2 = 2;
        String expectedErrorCode2 = I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode();
        String errorMessage2 = "TEST already existing resource : ";

        int testScenario3 = 3;
        String expectedErrorCode3 = I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode();
        String errorMessage3 = "TEST runtime exception while looking for the resource : ";

        return new Object[][]{
                {StringUtils.EMPTY, testChannel, testDomain, expectedErrorCode1, errorMessage1, testScenario1,
                        applicationUuid},
                {testTemplateName, testChannel, testDomain, expectedErrorCode2, errorMessage2, testScenario2,
                        applicationUuid},
                {testTemplateName, testChannel, testDomain, expectedErrorCode3, errorMessage3, testScenario3,
                        applicationUuid}
        };
    }

    /**
     * Contains the template details and the expected outcome for the scenarios.
     *
     * @return Object[][]
     * @throws Exception Error converting to a byte []
     */
    @DataProvider(name = "notificationTemplateDataProvider")
    private Object[][] notificationTemplateDataProvider() throws Exception {

        String locale = "en_US";
        String notificationTemplateType = "accountconfirmation";
        String charsetName = "UTF-8";
        String contentType = "html/plain";
        String applicationUuid1 = "test-uuid1";
        String applicationUuid2 = "test-uuid2";

        // Template 1: SMS.
        String notificationChannel1 = NotificationChannels.SMS_CHANNEL.getChannelType();
        String templateContentType1 = StringUtils.EMPTY;
        byte[] templateContent1 = "[\"body\"]".getBytes(charsetName);

        // Template 2: EMAIL.
        String notificationChannel2 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        byte[] templateContent2 = "[\"subject\",\"body\",\"footer\"]".getBytes(charsetName);

        return new Object[][] {
                { notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                        templateContentType1, templateContent1, applicationUuid1 },
                { notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                        contentType, templateContent2, applicationUuid2 }
        };
    }

    /**
     * Contains data for invalid requests.
     *
     * @return Object[][]
     */
    @DataProvider(name = "invalidNotificationTemplateDataProvider")
    private Object[][] invalidNotificationTemplateDataProvider() throws Exception{

        String locale = "en_US";
        String notificationTemplateType = "accountconfirmation";
        String charsetName = "UTF-8";
        String contentType = "html/plain";

        // Invalid template type.
        String errorMsg1 = "Invalid template type : ";
        String expectedErrorCode1 = I18nEmailUtil.prependOperationScenarioToErrorCode(
                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode(),
                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);

        // Invalid template locale.
        String errorMsg2 = "Invalid template locale : ";
        String expectedErrorCode2 =
                I18nEmailUtil.prependOperationScenarioToErrorCode(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getCode(),
                        I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);

        // Template 1: SMS.
        String notificationChannel1 = NotificationChannels.SMS_CHANNEL.getChannelType();
        String expectedErrorCode3 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT
                .getCode();
        byte[] templateContent1 = "[\"subject\",\"body\",\"footer\"]".getBytes(charsetName);

        // Template 2: EMAIL.
        String notificationChannel2 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        byte[] templateContent2 = "[\"body\"]".getBytes(charsetName);
        String expectedErrorCode4 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT
                .getCode();

        // No content in the EMAIL template.
        String notificationChannel3 = NotificationChannels.EMAIL_CHANNEL.getChannelType();
        String expectedErrorCode5 = IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE
                .getCode();

        // Invalid application UUID.
        String applicationUuid = "test-invalid-uuid";
        String errorMsg3 = "Invalid template locale : ";
        String expectedErrorCode6 =
                IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getCode();

        return new Object[][] {
                { StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, false, true, errorMsg1, expectedErrorCode1, null, null },
                { StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, true, false, errorMsg2, expectedErrorCode2, null, null },
                { notificationChannel1, notificationTemplateType, notificationTemplateType, locale,
                  StringUtils.EMPTY, true, true, StringUtils.EMPTY, expectedErrorCode3, templateContent1, null },
                { notificationChannel2, notificationTemplateType, notificationTemplateType, locale,
                  contentType, true, true, StringUtils.EMPTY, expectedErrorCode4, templateContent2, null },
                { notificationChannel3, notificationTemplateType, notificationTemplateType, locale,
                  contentType, true, true, StringUtils.EMPTY, expectedErrorCode5, null, null },
                { StringUtils.EMPTY, notificationTemplateType, notificationTemplateType, locale,
                StringUtils.EMPTY, true, true, errorMsg3, expectedErrorCode6, null, applicationUuid },
        };
    }

    /**
     * Build a NotificationTemplate model from the given input parameters.
     * NOTE: parameter order : displayName, channel, type, contentType, locale, body, subject, footer
     *
     * @return Notification Template model
     */
    private NotificationTemplate buildSampleNotificationTemplate(String[] templateContent) {

        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setNotificationChannel(templateContent[1]);
        notificationTemplate.setDisplayName(templateContent[0]);
        notificationTemplate.setType(templateContent[2]);
        notificationTemplate.setContentType(templateContent[3]);
        notificationTemplate.setLocale(templateContent[4]);
        notificationTemplate.setFooter(templateContent[7]);
        notificationTemplate.setBody(templateContent[5]);
        notificationTemplate.setSubject(templateContent[6]);
        return notificationTemplate;
    }

    /**
     * Mock registry resource for notification template.
     *
     * @param notificationChannel Notification channel
     * @param displayName         Notification template displayName
     * @param templateType        Notification template type
     * @param locale              Notification template locale
     * @param contentType         Notification template content type
     * @param templateContent     Notification template content (Subject,body,footer etc)
     * @param tenantDomain        Tenant domain of the notification template.
     * @param applicationId       Root application id of the notification template.
     * @throws Exception Error mocking notification template
     */
    private void mockRegistryResource(String notificationChannel, String displayName, String templateType,
                                      String locale, String contentType, byte[] templateContent, String tenantDomain,
                                      String applicationId)
            throws Exception {

        if (StringUtils.isNotBlank(tenantDomain) && StringUtils.isNotBlank(applicationId) &&
                StringUtils.equals(notificationChannel, NotificationChannels.EMAIL_CHANNEL.getChannelType())) {
            when(resourceMgtService.getIdentityResource(ArgumentMatchers.contains(applicationId),
                    ArgumentMatchers.eq(tenantDomain), ArgumentMatchers.anyString())).thenReturn(resource);
        } else if (StringUtils.isNotBlank(tenantDomain)) {
            when(resourceMgtService.getIdentityResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(tenantDomain),
                    ArgumentMatchers.anyString())).thenReturn(resource);
        } else {
            when(resourceMgtService.getIdentityResource(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString())).thenReturn(resource);
        }

        // Mock Resource properties.
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME)).thenReturn(displayName);
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_TYPE)).thenReturn(templateType);
        when(resource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE)).thenReturn(locale);
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            when(resource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE)).thenReturn(contentType);
        }
        when(resource.getContent()).thenReturn(templateContent);
    }

    /**
     * Mocks IdentityValidationUtil template validation methos.
     *
     * @param isValidTemplate Whether the template is valid or not
     * @param isValidLocale   Whether the locate is valid or not
     */
    private void mockIsValidTemplate(boolean isValidTemplate, boolean isValidLocale) {

        mockStatic(IdentityValidationUtil.class);

        // Mock methods in validateTemplateType method.
        when(IdentityValidationUtil
                .isValid(Matchers.anyString(), Matchers.any(String[].class), Matchers.any(String[].class)))
                .thenReturn(isValidTemplate);

        // Mock methods in validateLocale method.
        when(IdentityValidationUtil.isValidOverBlackListPatterns(Matchers.anyString(), Matchers.anyString())).
                thenReturn(isValidLocale);
    }

    /**
     * This method will validate the essential details of an Email or SMS notification template.
     *
     * @param notificationTemplate The notification template to be validated.
     * @param notificationChannel  The expected notification channel for the template being validated.
     */
    private void validateNotificationTemplate(NotificationTemplate notificationTemplate,
                                              String notificationChannel) {

        assertNotNull(notificationTemplate);
        assertNotNull(notificationTemplate.getBody(), "Template should have a notification body");
        assertEquals(notificationTemplate.getNotificationChannel(), notificationChannel);

        // Validate not having subject or footer in SMS notification template.
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            assertNull(notificationTemplate.getFooter(), "SMS notification template cannot have a footer");
            assertNull(notificationTemplate.getSubject(), "SMS notification template cannot have a subject");
        } else {
            assertNotNull(notificationTemplate.getFooter(), "EMAIL notification template must have a footer");
            assertNotNull(notificationTemplate.getSubject(),
                    "EMAIL notification template must have a subject");
        }
    }
}
