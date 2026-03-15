import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DriverTest {
  @Test
  void shouldRunDriverWithValidJar() {
    assertDoesNotThrow(() -> Driver.main(new String[] {"witup-test-jar-1.0-SNAPSHOT.jar"}));
  }
}
