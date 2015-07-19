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

@Mojo(name = "minify-js", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class MinifyJSMojo
    extends AbstractMojo {

  private static final String PATH_SEPARATOR = System.getProperty("file.separator");

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js/lib")
  private File externalJsDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.basedir}/src/main/webapp/js")
  private File jsDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}/js")
  private String outputFilePath;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "combined%d.min.js")
  private String outputFileName;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "false")
  private boolean compile;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.basedir}/src/main/webapp")
  private File htmlSourceDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
  private String htmlOutputDirectory;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "true")
  private boolean updateHTML;

  @SuppressWarnings("unused")
  @Parameter
  private List<String> jsCompileOrder;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "true")
  private boolean useHtmlForOrder;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "index.html")
  private String pagePattern;

  private int outputFileCount = 0;

  public void execute() throws MojoExecutionException {


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

    if (jsCompileOrder == null || jsCompileOrder.isEmpty()) {
      jsCompileOrder = new ArrayList<>();
      jsCompileOrder.add("**/*.js");
    }
    Map<String, List<String>> scriptsPerHtml = new HashMap<>();
    if (useHtmlForOrder){
      try {
        scriptsPerHtml = scriptTagsInHTML();
      } catch (Exception e) {
        throw new MojoExecutionException("Failed to scan html for script tags", e);
      }
    } else {
      scriptsPerHtml.put(wildcardToRegex(pagePattern), jsCompileOrder);
    }
    for (String pageKey : scriptsPerHtml.keySet()) {
      Set<File> projectSourceFiles = new LinkedHashSet<>();
      List<SourceFile> sourceFiles = new ArrayList<>();
      for (String filePattern : scriptsPerHtml.get(pageKey)) {
        for (File file : listFilesMatchingPattern(jsDirectory, wildcardToRegex(filePattern))) {
          if (!projectSourceFiles.contains(file)) {
            projectSourceFiles.add(file);
          }
        }
      }
      for (File file : projectSourceFiles) {
        sourceFiles.add(JSSourceFile.fromFile(file));
      }
      com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.INFO);
      com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
      Result result = compiler.compile(externalJavascriptFiles, sourceFiles, options);

      for (JSError message : compiler.getWarnings()) {
        getLog().debug("Warning message: " + message.toString());
      }

      for (JSError message : compiler.getErrors()) {
        getLog().debug("Error message: " + message.toString());
      }
      String finalOutputFileName = String.format(outputFileName, outputFileCount++);
      new File(outputFilePath).mkdirs();
      try (FileWriter outputFile = new FileWriter(new File(outputFilePath, finalOutputFileName))) {
        outputFile.write(compiler.toSource());
      } catch (Exception e) {
        throw new MojoExecutionException("Error while writing minified file", e);
      }
      if (updateHTML) {
        try {
          updateHTMLJSReferences(pageKey, sourceFiles, finalOutputFileName);
        } catch (Exception e) {
          throw new MojoExecutionException("Error updating HTML files", e);
        }
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
        if (file.isDirectory() && !file.getAbsolutePath().equals(externalJsDirectory.getAbsolutePath())
            && pattern.contains("/")) {
          String folder = pattern.substring(0, pattern.indexOf("/"));
          if (file.getName().matches(folder)) {
            String subPattern = pattern;
            if (!folder.equals(".*.*")) {
              subPattern = pattern.replace(folder + "/", "");
            }
            files.addAll(listFilesMatchingPattern(file, subPattern));
          }
        }
        if (!file.isDirectory()) {
          String filePattern = pattern;
          if (pattern.startsWith(".*.*/")){
            filePattern = pattern.substring(pattern.indexOf("/") + 1);
          }
          if (file.getName().matches(filePattern)) {
            files.add(file);
          }
        }
      }
    }
    return files;
  }

  private void updateHTMLJSReferences(final String pageRegex, List<SourceFile> projectSourceFiles,
      String finalOutputFileName) throws Exception {
    File[] htmlFiles = htmlSourceDirectory.listFiles(new FileFilter() {
      @Override public boolean accept(File pathname) {
        return (pathname.getName().matches(pageRegex));
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
              .replaceAll("<script.*?src=\"" + src + "\".*?></script>", "<script src=\"js/" + finalOutputFileName + "\"></script>");
          replaced = true;
        }
      }
      String outputHTMLFilename = htmlOutputDirectory + PATH_SEPARATOR + htmlFile.getName();
      try (FileWriter outputFile = new FileWriter(outputHTMLFilename)) {
        outputFile.write(content);
      }
    }
  }

  private Map<String, List<String>> scriptTagsInHTML() throws Exception {
    File[] htmlFiles = htmlSourceDirectory.listFiles(new FileFilter() {
      @Override public boolean accept(File pathname) {
        return (pathname.getName().matches(wildcardToRegex(pagePattern)));
      }
    });
    Map<String, List<String>> scriptsPerPage = new HashMap<>();
    for (File htmlFile : htmlFiles) {
      List<String> scripts = new ArrayList<>();
      String content = loadFileAsString(htmlFile);
      Pattern pattern = Pattern.compile("<script.*?src=\"(.*?js)\".*?></script>");
      Matcher matcher = pattern.matcher(content);
      while (matcher.find()) {
        String scriptFilename = matcher.group(1).substring(3);
        if (!scriptFilename.startsWith("lib/")) {
          scripts.add(matcher.group(1).substring(3));
        }
      }
      scriptsPerPage.put(htmlFile.getName(), scripts);
    }
    return scriptsPerPage;
  }

  private String loadFileAsString(File file) throws Exception {
    return IOUtil.toString(new FileInputStream(file)).replaceAll("\r", "").trim();
  }
}
