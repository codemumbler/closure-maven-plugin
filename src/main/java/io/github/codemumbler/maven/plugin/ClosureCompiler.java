package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.logging.Level;

public class ClosureCompiler {

  private final Log log;
  private boolean compile;
  private Compiler compiler;

  public ClosureCompiler(boolean compile, Log log) {
    this.compile = compile;
    this.log = log;
  }

  public void compile(List<SourceFile> externalJavascriptFiles, List<SourceFile> sourceFiles) {
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

  public String toSource() {
    return compiler.toSource();
  }
}
