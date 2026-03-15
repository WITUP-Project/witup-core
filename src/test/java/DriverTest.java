import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DriverTest {
  @Test
  void shouldRunDriverWithValidJar() {
    assertDoesNotThrow(() -> Driver.main(new String[] {"commons-lang3-3.21.0-SNAPSHOT.jar"}));
  }
}
