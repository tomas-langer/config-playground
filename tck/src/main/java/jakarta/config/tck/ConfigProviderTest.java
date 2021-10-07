/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.config.tck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Properties;

import jakarta.config.Config;
import jakarta.config.tck.base.AbstractTest;
import jakarta.inject.Inject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ConfigProviderTest extends Arquillian {

    private @Inject Config config;

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap
            .create(WebArchive.class, "configProviderTest.war")
            .addPackage(AbstractTest.class.getPackage())
            .addClass(ConfigProviderTest.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        AbstractTest.addFile(war, "META-INF/jakarta-config.properties");

        return war;
    }

    @BeforeClass
    public static void setupCheck() {
        // check that there is at least one property which is unique to the environment and not also a system property
        boolean checkOK = false;
        Map<String, String> env = System.getenv();
        Properties properties = System.getProperties();
        for (Map.Entry<String, String> envEntry : env.entrySet()) {
            String key = envEntry.getKey();
            if (!properties.containsKey(key)) {
                checkOK = true;
                break;
            }
        }
        Assert.assertTrue(checkOK, "Ensure that there is at least one property which is unique to " +
                "the environment variables and not also a system property.");
    }

    @Test
    public void testInjectedConfigSerializable() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream)) {
            out.writeObject(config);
        } catch (IOException ex) {
            Assert.fail("Injected config should be serializable, but could not serialize it", ex);
        }
        Object readObject = null;
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            readObject = in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Assert.fail(
                    "Injected config should be serializable, but could not deserialize a previously serialized instance",
                    ex);
        }
        MatcherAssert.assertThat("Deserialized object", readObject, CoreMatchers.instanceOf(Config.class));
    }
}
