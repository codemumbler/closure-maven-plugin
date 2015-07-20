package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.logging.Level;

class ClosureCompiler {

  private final Log log;
  private boolean compile;
  private Compiler compiler;

  ClosureCompiler(boolean compile, Log log) {
    this.compile = compile;
    this.log = log;
  }

  void compile(List<SourceFile> externalJavascriptFiles, List<SourceFile> sourceFiles) {
    CompilerOptions options = new CompilerOptions();
    if (!compile) {
      CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
    } else {
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
    }
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);
    com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.INFO);
    compiler = new com.google.javascript.jscomp.Compiler();
    compiler.compile(externalJavascriptFiles, sourceFiles, options);

    for (JSError message : compiler.getWarnings()) {
      log.debug("Warning message: " + message.toString());
    }

    for (JSError message : compiler.getErrors()) {
      log.debug("Error message: " + message.toString());
    }
  }

  public String saveCompiledSource(String outputFilePath, String outputFileName, int outputFileCount)
      throws MojoExecutionException {
    String finalOutputFileName = String.format(outputFileName, outputFileCount);
    new File(outputFilePath).mkdirs();
    try (FileWriter outputFile = new FileWriter(new File(outputFilePath, finalOutputFileName))) {
      outputFile.write(compiler.toSource());
    } catch (Exception e) {
      throw new MojoExecutionException("Error while writing minified file", e);
    }
    return finalOutputFileName;
  }
}
