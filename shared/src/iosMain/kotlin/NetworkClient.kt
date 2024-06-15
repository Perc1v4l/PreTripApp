import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.InternalAPI

class NetworkClient {
    private val client = HttpClient()

    @OptIn(InternalAPI::class)
    suspend fun sendHealthData(healthData: HealthData): HttpResponse {
        return client.post("https://192.168.1.90:8080/api/health/data") {
            contentType(ContentType.Application.Json)
            body = healthData
        }
    }
}