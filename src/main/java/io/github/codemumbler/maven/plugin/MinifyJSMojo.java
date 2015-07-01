package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Mojo(name = "minify-js", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class MinifyJSMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js/lib")
  private File externalJsDirectory;

  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js")
  private File jsDirectory;

  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}/js/combined.min.js")
  private String outputFilename;

  @Parameter(defaultValue = "false")
  private boolean compile;

  @Parameter(defaultValue = "${project.basedir}/src/main/webapp")
  private File htmlSourceDirectory;

  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
  private String htmlOutputDirectory;

  @Parameter(defaultValue = "true")
  private boolean updateHTML;

  public void execute() throws MojoExecutionException {
    com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.INFO);
    com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();

    CompilerOptions options = new CompilerOptions();
    if (!compile) {
      CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
    } else {
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
    }
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);

    List<SourceFile> externalJavascriptFiles = new ArrayList<>();
    if (externalJsDirectory != null && externalJsDirectory.listFiles() != null) {
      for (File file : externalJsDirectory.listFiles()) {
        externalJavascriptFiles.add(JSSourceFile.fromFile(file.getAbsolutePath()));
      }
    }

    List<SourceFile> projectSourceFiles = new ArrayList<>();
    for (File file : jsDirectory.listFiles()) {
      projectSourceFiles.add(JSSourceFile.fromFile(file.getAbsolutePath()));
    }

    compiler.compile(externalJavascriptFiles, projectSourceFiles, options);

    for (JSError message : compiler.getWarnings()) {
      System.err.println("Warning message: " + message.toString());
    }

    for (JSError message : compiler.getErrors()) {
      System.err.println("Error message: " + message.toString());
    }
    try (FileWriter outputFile = new FileWriter(outputFilename)) {
      outputFile.write(compiler.toSource());
    } catch (Exception e) {
      throw new MojoExecutionException("Error while writing minified file", e);
    }
    if (updateHTML) {
      try {
        updateHTMLJSReferences(projectSourceFiles);
      } catch (Exception e) {
        throw new MojoExecutionException("Error updating HTML files", e);
      }
    }
  }

  private void updateHTMLJSReferences(List<SourceFile> projectSourceFiles) throws Exception {
    File[] htmlFiles = htmlSourceDirectory.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return (pathname.getName().endsWith(".html"));
      }
    });
    for (File htmlFile : htmlFiles) {
      String content = loadFileAsString(htmlFile);
      content = content.replace("<script src=\"js/main.js\"></script>",
          "<script src=\"js/combined.min.js\"></script>");
      String outputHTMLFilename = htmlOutputDirectory + "/" + htmlFile.getName();
      try (FileWriter outputFile = new FileWriter(outputHTMLFilename)) {
        outputFile.write(content);
      }
    }
  }

  private String loadFileAsString(File file) throws Exception {
    return IOUtil.toString(new FileInputStream(file)).replaceAll("\r", "").trim();
  }
}
