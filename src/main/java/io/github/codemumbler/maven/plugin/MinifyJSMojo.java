package io.github.codemumbler.maven.plugin;

import com.google.javascript.jscomp.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Mojo(name = "minify-js", defaultPhase = LifecyclePhase.COMPILE)
public class MinifyJSMojo
		extends AbstractMojo {

	@Parameter(defaultValue = "${project.basedir}/src/main/webapp/js")
	private File jsDirectory;

	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}/js/combined.min.js")
	private String outputFilename;

	@Parameter(defaultValue = "false")
	private boolean compile;

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

		List<SourceFile> primaryJavascriptFiles = new ArrayList<>();
		for (File file : jsDirectory.listFiles()) {
			primaryJavascriptFiles.add(JSSourceFile.fromFile(file.getAbsolutePath()));
		}

		compiler.compile(externalJavascriptFiles, primaryJavascriptFiles, options);

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
	}
}
