package example

import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.testcontainers.containers.GenericContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest
class TasksSpec extends Specification implements TestPropertyProvider {

    @Shared
    @AutoCleanup
    public static GenericContainer<?> activeMQContainer = new GenericContainer<>("rmohr/activemq:5.15.9")
            .withExposedPorts(61616)

    @Inject
    @Client("/")
    HttpClient client

    PollingConditions pollingConditions = new PollingConditions(initialDelay: 2, timeout: 100)

    def 'should process tasks'() {
        expect:
            pollingConditions.eventually {
                client.toBlocking().retrieve("/tasks/processed-count", Integer) > 3
            }
    }

    @Override
    Map<String, String> getProperties() {
        activeMQContainer.start()
        return [
                "micronaut.jms.activemq.classic.connection-string": "tcp://${activeMQContainer.getHost()}:${activeMQContainer.getMappedPort(61616)}",
                "micronaut.jms.activemq.classic.username"         : "admin",
                "micronaut.jms.activemq.classic.password"         : "admin"
        ]
    }
}

