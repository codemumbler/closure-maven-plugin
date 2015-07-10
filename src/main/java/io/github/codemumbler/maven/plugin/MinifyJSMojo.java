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
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "minify-js", defaultPhase = LifecyclePhase.PREPARE_PACKAGE) public class MinifyJSMojo
    extends AbstractMojo {

  private static final String PATH_SEPARATOR = System.getProperty("file.separator");
  @SuppressWarnings("unused") @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js/lib") private File
      externalJsDirectory;

  @SuppressWarnings("unused") @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js") private File
      jsDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}/js/combined.min.js") private String
      outputFilename;

  @SuppressWarnings("unused") @Parameter(defaultValue = "false") private boolean compile;

  @SuppressWarnings("unused") @Parameter(defaultValue = "${project.basedir}/src/main/webapp") private File
      htmlSourceDirectory;

  @SuppressWarnings("unused") @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
  private String htmlOutputDirectory;

  @SuppressWarnings("unused") @Parameter(defaultValue = "true") private boolean updateHTML;

  @SuppressWarnings("unused") @Parameter private List<String> jsCompileOrder;

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
      File[] externalJSFiles = externalJsDirectory.listFiles(new FileFilter() {
        @Override public boolean accept(File file) {
          return !file.isDirectory() && file.getName().endsWith(".js");
        }
      });
      for (File file : externalJSFiles) {
        externalJavascriptFiles.add(JSSourceFile.fromFile(file.getAbsolutePath()));
      }
    }

    List<SourceFile> sourceFiles = new ArrayList<>();
    if (jsCompileOrder == null || jsCompileOrder.isEmpty()) {
      jsCompileOrder = new ArrayList<>();
      jsCompileOrder.add(".*\\.js");
    }
    Set<File> projectSourceFiles = new LinkedHashSet<>();
    for (String filePattern : jsCompileOrder) {
      for (File file : listFilesMatchingPattern(jsDirectory, wildcardToRegex(filePattern))) {
        if (!projectSourceFiles.contains(file)) {
          projectSourceFiles.add(file);
        }
      }
    }
    for (File file : projectSourceFiles) {
      sourceFiles.add(JSSourceFile.fromFile(file));
    }

    compiler.compile(externalJavascriptFiles, sourceFiles, options);

    for (JSError message : compiler.getWarnings()) {
      System.err.println("Warning message: " + message.toString());
    }

    for (JSError message : compiler.getErrors()) {
      System.err.println("Error message: " + message.toString());
    }
    String outputPath = outputFilename.substring(0, outputFilename.lastIndexOf("/"));
    new File(outputPath).mkdirs();
    try (FileWriter outputFile = new FileWriter(outputFilename)) {
      outputFile.write(compiler.toSource());
    } catch (Exception e) {
      throw new MojoExecutionException("Error while writing minified file", e);
    }
    if (updateHTML) {
      try {
        updateHTMLJSReferences(sourceFiles);
      } catch (Exception e) {
        throw new MojoExecutionException("Error updating HTML files", e);
      }
    }
  }

  private String wildcardToRegex(String filePattern) {
    return filePattern.replaceAll("\\.", "\\.").replaceAll("\\*", ".*");
  }

  private List<File> listFilesMatchingPattern(final File parentDirectory, String pattern) {
    List<File> files = new ArrayList<>();
    File[] listOfFiles = parentDirectory.listFiles();
    if (listOfFiles != null) {
      for (File file : listOfFiles) {
        if (file.isDirectory() && !file.getAbsolutePath().equals(externalJsDirectory.getAbsolutePath())) {
          files.addAll(listFilesMatchingPattern(file, pattern));
        }
        if (file.getName().matches(pattern)) {
          files.add(file);
        }
      }
    }
    return files;
  }

  private void updateHTMLJSReferences(List<SourceFile> projectSourceFiles) throws Exception {
    File[] htmlFiles = htmlSourceDirectory.listFiles(new FileFilter() {
      @Override public boolean accept(File pathname) {
        return (pathname.getName().endsWith(".html"));
      }
    });

    for (File htmlFile : htmlFiles) {
      String content = loadFileAsString(htmlFile);
      boolean replaced = false;
      for (SourceFile jsSourceFile : projectSourceFiles) {
        String src = jsSourceFile.getName().replace(htmlFile.getParentFile().getAbsolutePath() + PATH_SEPARATOR, "");
        src = src.replace("\\", "/");
        Pattern pattern = Pattern.compile("<script.*?src=\"" + src + "\".*?></script>");
        Matcher matcher = pattern.matcher(content);
        if (replaced) {
          content = content.replaceAll("\\s*<script.*?src=\"" + src + "\".*?></script>", "");
          continue;
        }
        if (matcher.find()) {
          content = content
              .replaceAll("<script.*?src=\"" + src + "\".*?></script>", "<script src=\"js/combined.min.js\"></script>");
          replaced = true;
        }
      }
      String outputHTMLFilename = htmlOutputDirectory + PATH_SEPARATOR + htmlFile.getName();
      try (FileWriter outputFile = new FileWriter(outputHTMLFilename)) {
        outputFile.write(content);
      }
    }
  }

  private String loadFileAsString(File file) throws Exception {
    return IOUtil.toString(new FileInputStream(file)).replaceAll("\r", "").trim();
  }
}
