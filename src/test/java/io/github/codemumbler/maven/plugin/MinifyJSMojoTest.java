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
    new File("target/test-classes/one-js/target/one-js/js").mkdirs();
    executeMojo("target/test-classes/one-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/one-js/expected.js"),
        loadFileAsString("target/test-classes/one-js/target/one-js/js/combined.min.js"));
  }

  @Test
  public void twoJS() throws Exception {
    new File("target/test-classes/two-js/target/two-js/js").mkdirs();
    executeMojo("target/test-classes/two-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/two-js/expected.js"),
        loadFileAsString("target/test-classes/two-js/target/two-js/js/combined.min.js"));
  }

  @Test
  public void compile() throws Exception {
    new File("target/test-classes/two-js/target/two-js/js").mkdirs();
    executeMojo("target/test-classes/two-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/two-js/expected.js"),
        loadFileAsString("target/test-classes/two-js/target/two-js/js/combined.min.js"));
  }

  @Test
  public void externalLibrariesJS() throws Exception {
    new File("target/test-classes/lib-js/target/lib-js/js").mkdirs();
    executeMojo("target/test-classes/lib-js");
    Assert.assertEquals(loadFileAsString("target/test-classes/lib-js/expected.js"),
        loadFileAsString("target/test-classes/lib-js/target/lib-js/js/combined.min.js"));
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