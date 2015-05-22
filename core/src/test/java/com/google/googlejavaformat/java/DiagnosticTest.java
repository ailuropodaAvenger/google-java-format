/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.googlejavaformat.java;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for error reporting.
 */
@RunWith(JUnit4.class)
public class DiagnosticTest {
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void parseError() throws Exception {
    String input =
        Joiner.on('\n')
            .join(
                "public class InvalidSyntax {",
                "  private static NumPrinter {",
                "    public static void print(int n) {",
                "      System.out.printf(\"%d%n\", n);",
                "    }",
                "  }",
                "",
                "  public static void main(String[] args) {",
                "    NumPrinter.print(args.length);",
                "  }",
                "}");

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true));

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("InvalidSyntax.java");
    Files.write(path, input.getBytes(UTF_8));

    int result = main.format(path.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString())
        .contains("InvalidSyntax.java:2: error: Syntax error on token \"NumPrinter\"");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void lexError() throws Exception {
    String input = "\\uuuuuuuuuuuuuuuuuuuuuuuuuuuuuu00not-actually-a-unicode-escape-sequence";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true));

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("InvalidSyntax.java");
    Files.write(path, input.getBytes(UTF_8));

    int result = main.format(path.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString()).contains("InvalidSyntax.java: error: Invalid_Unicode_Escape");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void oneFileParseError() throws Exception {
    String one = "class One {\n";
    String two = "class Two {}\n";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true));

    Path tmpdir = testFolder.newFolder().toPath();
    Path pathOne = tmpdir.resolve("One.java");
    Files.write(pathOne, one.getBytes(UTF_8));

    Path pathTwo = tmpdir.resolve("Two.java");
    Files.write(pathTwo, two.getBytes(UTF_8));

    int result = main.format(pathOne.toString(), pathTwo.toString());
    assertThat(stdout.toString()).isEqualTo(two);
    assertThat(stderr.toString()).contains("One.java:1: error: Syntax error, insert \"}\"");
    assertThat(result).isEqualTo(1);
  }
  
  @Test
  public void oneFileParseErrorReplace() throws Exception {
    String one = "class One {}}\n";
    String two = "class Two {\n}\n";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true));

    Path tmpdir = testFolder.newFolder().toPath();
    Path pathOne = tmpdir.resolve("One.java");
    Files.write(pathOne, one.getBytes(UTF_8));

    Path pathTwo = tmpdir.resolve("Two.java");
    Files.write(pathTwo, two.getBytes(UTF_8));

    int result = main.format("-i", pathOne.toString(), pathTwo.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString()).contains("One.java:1: error: Syntax error on token \"}\"");
    assertThat(result).isEqualTo(1);
    // don't edit files with parse errors
    assertThat(Files.readAllLines(pathOne, UTF_8)).containsExactly("class One {}}");
    assertThat(Files.readAllLines(pathTwo, UTF_8)).containsExactly("class Two {}");
  }
}