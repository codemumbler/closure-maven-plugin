package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

@Mojo(name = "minify-js", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class MinifyJSMojo
    extends AbstractMojo {

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

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "false")
  private boolean generateMap;

  private int outputFileCount = 0;

  public void execute() throws MojoExecutionException {

    List<SourceFile> externalJavascriptFiles = buildJsLibraryList();

    if (jsCompileOrder == null || jsCompileOrder.isEmpty()) {
      jsCompileOrder = new ArrayList<>();
      jsCompileOrder.add("**/*.js");
    }
    HtmlParser htmlParser = new HtmlParser(htmlOutputDirectory, htmlSourceDirectory);
    Map<String, List<String>> scriptsPerHtml = new HashMap<>();
    if (useHtmlForOrder){
      scriptsPerHtml = htmlParser.scriptTagsInHTML(pagePattern);
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
      ClosureCompiler compiler = new ClosureCompiler(compile, outputFilePath, getLog());
      compiler.compile(externalJavascriptFiles, sourceFiles, generateMap);
      String finalOutputFileName = compiler.saveCompiledSource(outputFileName, outputFileCount++);
      if (updateHTML) {
        htmlParser.updateHtmlJsReferences(pageKey, sourceFiles, finalOutputFileName);
      }
      if (generateMap) {
        String finalMapOutputFileName = finalOutputFileName + ".map";
        compiler.saveMap(finalMapOutputFileName);
      }
    }
  }

  private List<SourceFile> buildJsLibraryList() {
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
    return externalJavascriptFiles;
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
}
