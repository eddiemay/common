package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FormatTextTest {
  @Test
  public void toLowerCamel() {
    assertThat(FormatText.toLowerCamel("hello_there")).isEqualTo("helloThere");
    assertThat(FormatText.toLowerCamel("hello.there")).isEqualTo("helloThere");
    assertThat(FormatText.toLowerCamel("HELLO_THERE")).isEqualTo("helloThere");
    assertThat(FormatText.toLowerCamel("helloThere")).isEqualTo("helloThere");
    assertThat(FormatText.toLowerCamel("HelloThere")).isEqualTo("helloThere");
  }

  @Test
  public void toUpperCamel() {
    assertThat(FormatText.toUpperCamel("hello_there")).isEqualTo("HelloThere");
    assertThat(FormatText.toUpperCamel("hello.there")).isEqualTo("HelloThere");
    assertThat(FormatText.toUpperCamel("HELLO_THERE")).isEqualTo("HelloThere");
    // assertThat(FormatText.toUpperCamel("helloThere")).isEqualTo("HelloThere");
    // assertThat(FormatText.toUpperCamel("HelloThere")).isEqualTo("HelloThere");
  }
}
