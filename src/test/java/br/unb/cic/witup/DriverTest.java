package br.unb.cic.witup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DriverTest {
  @Test
  void shouldRunDriverWithValidJar() {
    assertDoesNotThrow(() -> Driver.main(new String[] {"witup-test-jar-0.0.2-SNAPSHOT.jar"}));
  }
}
