package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

class ClosureCompiler {

  private final Log log;
  private final boolean compile;
  private Compiler compiler;
  private String outputFilePath;

  ClosureCompiler(boolean compile, String outputFilePath, Log log) {
    this.compile = compile;
    this.log = log;
    this.outputFilePath = outputFilePath;
  }

  void compile(List<SourceFile> externalJavascriptFiles, List<SourceFile> sourceFiles, boolean generateMap) {
    CompilerOptions options = new CompilerOptions();
    if (!compile) {
      CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
    } else {
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
    }
    if (generateMap) {
      options.setSourceMapOutputPath(outputFilePath);
    }
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);
    com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.SEVERE);
    compiler = new com.google.javascript.jscomp.Compiler();
    compiler.compile(externalJavascriptFiles, sourceFiles, options);

    for (JSError message : compiler.getWarnings()) {
      log.debug("Warning message: " + message.toString());
    }

    for (JSError message : compiler.getErrors()) {
      log.debug("Error message: " + message.toString());
    }
  }

  String saveCompiledSource(String outputFileName, int outputFileCount) throws MojoExecutionException {
    String finalOutputFileName = String.format(outputFileName, outputFileCount);
    new File(outputFilePath).mkdirs();
    try (FileWriter outputFile = new FileWriter(new File(outputFilePath, finalOutputFileName))) {
      outputFile.write(compiler.toSource());
    } catch (Exception e) {
      throw new MojoExecutionException("Error while writing minified file", e);
    }
    return finalOutputFileName;
  }

  public void saveMap(String finalMapOutputFileName) throws MojoExecutionException {
    try (FileWriter f = new FileWriter(new File(outputFilePath, finalMapOutputFileName))) {
      compiler.getSourceMap().appendTo(f, finalMapOutputFileName);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
