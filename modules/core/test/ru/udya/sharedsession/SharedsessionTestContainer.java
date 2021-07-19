package ru.udya.sharedsession;

import com.haulmont.cuba.testsupport.TestContainer;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

public class SharedsessionTestContainer extends TestContainer {

    public SharedsessionTestContainer() {
        super();
        springConfig = "ru/udya/sharedsession/test-spring.xml";
        appComponents = Arrays.asList(
                "com.haulmont.cuba",
                "ru.udya.querydsl.cuba",
                "com.haulmont.addon.restapi");
        appPropertiesFiles = Arrays.asList(
                // List the files defined in your web.xml
                // in appPropertiesConfig context parameter of the core module
                "ru/udya/sharedsession/app.properties",
                // Add this file which is located in CUBA and defines some properties
                // specifically for test environment. You can replace it with your own
                // or add another one in the end.
                "com/haulmont/cuba/testsupport/test-app.properties");
        autoConfigureDataSource();
    }

    public static class Common extends SharedsessionTestContainer {

        public static final SharedsessionTestContainer.Common INSTANCE = new SharedsessionTestContainer.Common();

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
}
