# closure-maven-plugin
A Google Closure Compiler Maven plugin to compile and minify javascript in a Java webapp.


Recommended usage:
```
  <plugin>
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-war-plugin</artifactId>
		<executions>
			<execution>
				<id>prepare-package</id>
				<phase>prepare-package</phase>
				<goals>
					<goal>exploded</goal>
				</goals>
			</execution>
			<execution>
				<id>default-war</id>
				<phase>package</phase>
				<goals>
					<goal>war</goal>
				</goals>
				<configuration>
					<warSourceExcludes>**</warSourceExcludes>
				</configuration>
			</execution>
		</executions>
	</plugin>
	<plugin>
		<groupId>io.github.codemumbler.maven.plugin</groupId>
		<artifactId>closure-maven-plugin</artifactId>
		<executions>
			<execution>
				<phase>prepare-package</phase>
				<goals>
					<goal>minify-js</goal>
				</goals>
			</execution>
		</executions>
	</plugin>
```
