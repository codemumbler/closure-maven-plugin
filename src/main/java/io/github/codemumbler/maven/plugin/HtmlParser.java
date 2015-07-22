package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.SourceFile;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlParser {

  private static final String PATH_SEPARATOR = System.getProperty("file.separator");
  private final String htmlOutputDirectory;
  private final File htmlSourceDirectory;
  private final String pagePattern;

  HtmlParser(String htmlOutputDirectory, File htmlSourceDirectory, String pagePattern) {
    this.htmlOutputDirectory = htmlOutputDirectory;
    this.htmlSourceDirectory = htmlSourceDirectory;
    this.pagePattern = pagePattern;
  }

  void updateHtmlJsReferences(final String pageRegex, List<SourceFile> projectSourceFiles, String finalOutputFilename)
      throws MojoExecutionException {
    try {
      updateHtml(pageRegex, projectSourceFiles, finalOutputFilename);
    } catch (Exception e) {
      throw new MojoExecutionException("Error updating HTML files", e);
    }
  }

  void updateHtml(final String pageRegex, List<SourceFile> projectSourceFiles, String finalOutputFileName) throws Exception {
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
          content = content.replaceAll("<script.*?src=\"" + src + "\".*?></script>",
              "<script src=\"js/" + finalOutputFileName + "\"></script>");
          replaced = true;
        }
      }
      String outputHTMLFilename = htmlOutputDirectory + PATH_SEPARATOR + htmlFile.getName();
      try (FileWriter outputFile = new FileWriter(outputHTMLFilename)) {
        outputFile.write(content);
      }
    }
  }

  Map<String, List<String>> scriptTagsInHTML() throws MojoExecutionException {
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

  private String wildcardToRegex(String filePattern) {
    return filePattern.replaceAll("\\.", "\\.").replaceAll("\\*", ".*");
  }

  private String loadFileAsString(File file) throws MojoExecutionException {
    try {
      return IOUtil.toString(new FileInputStream(file)).replaceAll("\r", "").trim();
    } catch (Exception e) {
      throw new MojoExecutionException("Could not read from file");
    }
  }
}
