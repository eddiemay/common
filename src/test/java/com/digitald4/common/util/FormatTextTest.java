package com.digitald4.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormatTextTest {
  @Test
  public void toLowerCamel() {
    assertEquals("helloThere", FormatText.toLowerCamel("hello_there"));
    assertEquals("helloThere", FormatText.toLowerCamel("hello.there"));
    assertEquals("helloThere", FormatText.toLowerCamel("HELLO_THERE"));
    assertEquals("helloThere", FormatText.toLowerCamel("helloThere"));
    assertEquals("helloThere", FormatText.toLowerCamel("HelloThere"));
  }
}
