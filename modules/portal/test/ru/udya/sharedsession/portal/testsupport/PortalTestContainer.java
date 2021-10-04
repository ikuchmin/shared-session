package ru.udya.sharedsession.portal.testsupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Strings;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.sys.AbstractAppContextLoader;
import com.haulmont.cuba.core.sys.AppComponents;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.CubaClassPathXmlApplicationContext;
import com.haulmont.cuba.core.sys.events.AppContextInitializedEvent;
import com.haulmont.cuba.core.sys.persistence.EclipseLinkCustomizer;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Inspired by {@link  com.haulmont.cuba.web.testsupport.TestContainer}
 */
public class PortalTestContainer extends ExternalResource implements BeforeAllCallback, AfterAllCallback {

    public static class Common extends PortalTestContainer {

        public static final PortalTestContainer.Common INSTANCE = new PortalTestContainer.Common();

        private static volatile boolean initialized;

        private Common() {
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            if (!initialized) {
                super.beforeAll(extensionContext);
                initialized = true;
            }
            setupContext();
        }


        @SuppressWarnings("RedundantThrows")
        @Override
        public void afterAll(ExtensionContext extensionContext) throws Exception {
            cleanupContext();
            // never stops - do not call super
        }
    }

    private final Logger log;

    protected String springConfig;
    protected List<String> appComponents;
    protected List<String> appPropertiesFiles;

    private ClassPathXmlApplicationContext springAppContext;
    private final Map<String, String> appProperties = new HashMap<>();

    public PortalTestContainer() {
        String property = System.getProperty("logback.configurationFile");
        if (StringUtils.isBlank(property)) {
            System.setProperty("logback.configurationFile", "com/haulmont/cuba/testsupport/test-logback.xml");
        }
        log = LoggerFactory.getLogger(PortalTestContainer.class);
        springConfig = "ru/udya/sharedsession/portal/testsupport/test-spring.xml";
        appComponents = new ArrayList<>(Arrays.asList(
                "com.haulmont.cuba",
                "com.haulmont.addon.restapi"
        ));
        appPropertiesFiles = Arrays.asList(
                "ru/udya/sharedsession/portal-app.properties",
                "ru/udya/sharedsession/portal/testsupport/test-portal-app.properties");
    }

    public void setupLogging(String logger, Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(logger).setLevel(level);
    }

    public List<String> getAppComponents() {
        return appComponents;
    }

    public List<String> getAppPropertiesFiles() {
        return appPropertiesFiles;
    }

    public String getSpringConfig() {
        return springConfig;
    }

    public PortalTestContainer setSpringConfig(String springConfig) {
        this.springConfig = springConfig;
        return this;
    }

    public PortalTestContainer setAppComponents(List<String> appComponents) {
        this.appComponents = appComponents;
        return this;
    }

    public PortalTestContainer setAppPropertiesFiles(List<String> appPropertiesFiles) {
        this.appPropertiesFiles = appPropertiesFiles;
        return this;
    }

    public PortalTestContainer addAppPropertiesFile(String name) {
        ArrayList<String> list = new ArrayList<>(appPropertiesFiles);
        list.add(name);
        this.appPropertiesFiles = list;
        return this;
    }

    public ClassPathXmlApplicationContext getSpringAppContext() {
        return springAppContext;
    }

    public Map<String, String> getAppProperties() {
        return appProperties;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        after();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        try {
            before();
        } catch (Throwable throwable) {
            log.error("TestContainer extension initialization failed.", throwable);
        }
    }

    @Override
    protected void before() {
        log.info("Starting test container " + this);
        System.setProperty("cuba.unitTestMode", "true");

        initAppComponents();
        initAppProperties();
        for (Map.Entry<String, String> entry : appProperties.entrySet()) {
            AppContext.setProperty(entry.getKey(), entry.getValue());
        }

        initAppContext();
    }

    @Override
    protected void after() {
        log.info("Stopping test container " + this);
        try {
            ((ConfigurableApplicationContext) AppContext.getApplicationContext()).close();
            AppContext.Internals.setApplicationContext(null);
            for (String name : AppContext.getPropertyNames()) {
                AppContext.setProperty(name, null);
            }
        } catch (Exception e) {
            log.warn("Error closing test container", e);
        }
    }

    protected void initAppComponents() {
        AppContext.Internals.setAppComponents(new AppComponents(getAppComponents(), "portal"));
    }

    protected void initAppProperties() {
        Properties properties = new Properties();

        List<String> locations = getAppPropertiesFiles();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        for (String location : locations) {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                try (InputStream stream = resource.getInputStream()) {
                    BOMInputStream bomInputStream = new BOMInputStream(stream);
                    properties.load(bomInputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read app properties " + location, e);
                }
            } else {
                log.warn("Resource {} not found, ignore it", location);
            }
        }

        StringSubstitutor substitutor = new StringSubstitutor(key -> {
            String subst = properties.getProperty(key);
            return subst != null ? subst : System.getProperty(key);
        });

        for (Object key : properties.keySet()) {
            String value = substitutor.replace(properties.getProperty((String) key));
            appProperties.put((String) key, value);
        }

        File dir;
        dir = new File(appProperties.get("cuba.confDir"));
        dir.mkdirs();
        dir = new File(appProperties.get("cuba.logDir"));
        dir.mkdirs();
        dir = new File(appProperties.get("cuba.tempDir"));
        dir.mkdirs();
        dir = new File(appProperties.get("cuba.dataDir"));
        dir.mkdirs();
    }

    protected void initAppContext() {
        EclipseLinkCustomizer.initTransientCompatibleAnnotations();

        String configProperty = AppContext.getProperty(AbstractAppContextLoader.SPRING_CONTEXT_CONFIG);

        org.apache.commons.text.StringTokenizer tokenizer = new StringTokenizer(configProperty);
        List<String> locations = tokenizer.getTokenList();
        String springConfig = getSpringConfig();
        if (!Strings.isNullOrEmpty(springConfig) && !locations.contains(springConfig)) {
            locations.add(springConfig);
        }

        springAppContext = new CubaClassPathXmlApplicationContext(locations.toArray(new String[0]));
        AppContext.Internals.setApplicationContext(springAppContext);

        Events events = springAppContext.getBean(Events.class);
        events.publish(new AppContextInitializedEvent(springAppContext));
    }

    protected void cleanupContext() {
        AppContext.Internals.setApplicationContext(null);
        for (String name : AppContext.getPropertyNames()) {
            AppContext.setProperty(name, null);
        }
    }

    protected void setupContext() {
        AppContext.Internals.setApplicationContext(getSpringAppContext());
        for (Map.Entry<String, String> entry : getAppProperties().entrySet()) {
            AppContext.setProperty(entry.getKey(), entry.getValue());
        }
    }
}
