import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters
import org.junit.Test

class IntegrationAPITests {

    val broker: BrokerProxy = BrokerProxy.getInstance(
        IntegrationNodeConnectionParameters(
            System.getenv("iibhost"),
            System.getenv("iibport").toInt(),
            System.getenv("iibuser"),
            System.getenv("iibpassword"),
            true
        ).apply { setAdvancedConnectionParameters(10, 5000) }
    ).apply {
        synchronous = 60000
    }

    @Test
    fun explore(){
        println(broker.name)
    }
}