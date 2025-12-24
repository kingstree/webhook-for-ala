package aladin.webhook.testkit

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Base class for pure unit tests.
 *
 * - Does NOT start Spring context
 * - Enables Mockito annotations (@Mock, @Spy, @InjectMocks)
 */
@ExtendWith(MockitoExtension::class)
abstract class UnitTestBase {
}
