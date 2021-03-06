package io.github.codemumbler.maven.plugin;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

public class MinifyJSMojoTest {

  private static final String GOAL = "minify-js";
  private MinifyJSMojo mojo;

  @Rule
  public MojoRule rule = new MojoRule() {
    @Override
    protected void before() throws Throwable {
    }

    @Override
    protected void after() {
    }
  };

  @Test
  public void oneJS() throws Exception {
    executeMojo("target/test-classes/one-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/one-js/expected.js"),
        loadFileAsString("target/test-classes/one-js/target/one-js/js/combined0.min.js"));
  }

  @Test
  public void twoJS() throws Exception {
    executeMojo("target/test-classes/two-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/two-js/expected.js"),
        loadFileAsString("target/test-classes/two-js/target/two-js/js/combined0.min.js"));
  }

  @Test
  public void subfolderJS() throws Exception {
    executeMojo("target/test-classes/subfolder-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/subfolder-js/expected.js"),
        loadFileAsString("target/test-classes/subfolder-js/target/subfolder-js/js/combined0.min.js"));
  }

  @Test
  public void compile() throws Exception {
    executeMojo("target/test-classes/two-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/two-js/expected.js"),
        loadFileAsString("target/test-classes/two-js/target/two-js/js/combined0.min.js"));
  }

  @Test
  public void externalLibrariesJS() throws Exception {
    executeMojo("target/test-classes/lib-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/lib-js/expected.js"),
        loadFileAsString("target/test-classes/lib-js/target/lib-js/js/combined0.min.js"));
  }

  @Test
  public void orderedJS() throws Exception {
    executeMojo("target/test-classes/multi-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/multi-js/expected.js"),
        loadFileAsString("target/test-classes/multi-js/target/multi-js/js/combined0.min.js"));
  }

  @Test
  public void updateHTMLPage_updatedOneJs() throws Exception {
    executeMojo("target/test-classes/one-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/one-js/expected.html"),
        loadFileAsString("target/test-classes/one-js/target/one-js/index.jsp"));
  }

  @Test
  public void updateHTMLTestPage_updatedOneJs() throws Exception {
    executeMojo("target/test-classes/one-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/one-js/expected-tests.jsp"),
        loadFileAsString("target/test-classes/one-js/target/one-js/tests.jsp"));
  }

  @Test
  public void updateHTMLPage_updatedTwoJs() throws Exception {
    executeMojo("target/test-classes/two-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/two-js/expected.html"),
        loadFileAsString("target/test-classes/two-js/target/two-js/index.html"));
  }

  @Test
  public void updateHTMLPage_updatedLibJs() throws Exception {
    executeMojo("target/test-classes/lib-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/lib-js/expected.html"),
        loadFileAsString("target/test-classes/lib-js/target/lib-js/index.html"));
  }

  @Test
  public void generateMapFile() throws Exception {
    executeMojo("target/test-classes/one-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/one-js/expected.js.map"),
        loadFileAsString("target/test-classes/one-js/target/one-js/js/combined0.min.js.map"));
  }

  private String loadFileAsString(String fileName) throws Exception {
    return IOUtil.toString(new FileInputStream(fileName)).replaceAll("\r", "").trim();
  }

  private void executeMojo(String pomFile) throws Exception {
    File pom = new File(pomFile);
    mojo = (MinifyJSMojo) rule.lookupConfiguredMojo(pom, GOAL);
    mojo.execute();
  }
}
