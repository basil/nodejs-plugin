/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs.tools;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;

@RunWith(Parameterized.class)
public class NodeJSInstallerProxyTest {

    @Parameters(name = "proxy url = {0}")
    public static String[][] data() throws MalformedURLException {
        return new String[][] { { "http://proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" },
            { "http://user:password@proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" }
        };
    }

    @Rule
    public JenkinsRule r = new JenkinsRule();
    private String host;
    private int port;
    private String username;
    private String password;
    private String expectedURL;
    private TaskListener log;
    private String noProxy;

    public NodeJSInstallerProxyTest(String url, String noProxy) throws Exception {
        URL proxyURL = new URL(url);

        this.log = new StreamBuildListener(System.out, Charset.defaultCharset());
        this.expectedURL = url;
        this.noProxy = noProxy;
        this.host = proxyURL.getHost();
        this.port = proxyURL.getPort();
        if (proxyURL.getUserInfo() != null) {
            String[] userInfo = proxyURL.getUserInfo().split(":");
            this.username = userInfo[0];
            this.password = userInfo[1];
        }
    }

    @Issue("JENKINS-29266")
    @Test
    public void test_proxy_settings() throws Exception {
        r.getInstance().proxy = new ProxyConfiguration(host, port, username, password);

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Method method = installer.getClass().getDeclaredMethod("buildProxyEnvVars", EnvVars.class, TaskListener.class);
        method.setAccessible(true);
        method.invoke(installer, env, log);

        Assertions.assertThat(env.keySet()).contains("HTTP_PROXY", "HTTPS_PROXY");
        Assertions.assertThat(env.get("HTTP_PROXY")).isEqualTo(expectedURL);
        Assertions.assertThat(env.get("HTTPS_PROXY")).isEqualTo(expectedURL);
        Assertions.assertThat(env.keySet()).doesNotContain("NO_PROXY");
    }

    @Test
    public void test_no_proxy_settings() throws Exception {
        r.getInstance().proxy = new ProxyConfiguration(host, port, username, password, noProxy);

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Method method = installer.getClass().getDeclaredMethod("buildProxyEnvVars", EnvVars.class, TaskListener.class);
        method.setAccessible(true);
        method.invoke(installer, env, log);

        Assertions.assertThat(env.keySet()).contains("HTTP_PROXY", "HTTPS_PROXY");
        Assertions.assertThat(env.get("NO_PROXY")).isEqualTo("*.npm.org,registry.npm.org");
    }

}