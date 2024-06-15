import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.ios.Ios
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.InternalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.distantPast
import platform.HealthKit.*
import platform.UIKit.UIDevice

@Serializable
data class HealthData(
    val pulse: Int,
    val blood_pressure_systolic: Int,
    val blood_pressure_diastolic: Int,
    val temperature: Double,
    val blood_alcohol_level: Double,
    val idfv: String
)

class HealthKitManager {
    private val healthStore: HKHealthStore = HKHealthStore()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json{
                prettyPrint = true
                isLenient = true
            })
        }
    }

    fun isHealthDataAvailable(): Boolean {
        return HKHealthStore.isHealthDataAvailable()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun requestAuthorization(): Boolean {
        val readTypes = setOf(
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)!!,
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBloodPressureSystolic)!!,
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBloodPressureDiastolic)!!,
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyTemperature)!!,
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBloodAlcoholContent)!!
        )

        return suspendCancellableCoroutine { continuation ->
            healthStore.requestAuthorizationToShareTypes(null, readTypes) { success, _ ->
                if (success) {
                    continuation.resume(true) {}
                } else {
                    continuation.resume(false) {}
                }
            }
        }
    }

    suspend fun getHealthData(): HealthData {
        val heartRates = getSingleQuantitySample(HKQuantityTypeIdentifierHeartRate.toString(), HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit()))
        val bloodPressureSystolic = getSingleQuantitySample(HKQuantityTypeIdentifierBloodPressureSystolic.toString(), HKUnit.millimeterOfMercuryUnit())
        val bloodPressureDiastolic = getSingleQuantitySample(HKQuantityTypeIdentifierBloodPressureDiastolic.toString(), HKUnit.millimeterOfMercuryUnit())
        val bodyTemperature = getSingleQuantitySampleDouble(HKQuantityTypeIdentifierBodyTemperature.toString(), HKUnit.degreeCelsiusUnit())
        val bloodAlcoholContent = getSingleQuantitySampleDouble(HKQuantityTypeIdentifierBloodAlcoholContent.toString(), HKUnit.percentUnit())

        val deviceID = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: ""

        return HealthData(
            pulse = heartRates,
            blood_pressure_systolic = bloodPressureSystolic,
            blood_pressure_diastolic = bloodPressureDiastolic,
            temperature = bodyTemperature.toDouble(),
            blood_alcohol_level = bloodAlcoholContent.toDouble(),
            idfv = deviceID
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getSingleQuantitySample(identifier: String, unit: HKUnit): Int {
        val quantityType = HKObjectType.quantityTypeForIdentifier(identifier)!!
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            NSDate.distantPast,
            NSDate(),
            HKQueryOptionNone
        )

        return suspendCancellableCoroutine { continuation ->
            val query = HKSampleQuery(quantityType, predicate, 0u, null) { _, results, error ->
                if (error != null || results == null || results.isEmpty()) {
                    continuation.resume(0) {} // Return 0 or handle error case appropriately
                } else {
                    val sample = results.filterIsInstance<HKQuantitySample>().firstOrNull()
                    if (sample != null) {
                        continuation.resume(sample.quantity.doubleValueForUnit(unit).toInt()) {}
                    } else {
                        continuation.resume(0) {} // Return 0 or handle case where sample is null
                    }
                }
            }
            healthStore.executeQuery(query)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getSingleQuantitySampleDouble(identifier: String, unit: HKUnit): Double {
        val quantityType = HKObjectType.quantityTypeForIdentifier(identifier)!!
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            NSDate.distantPast,
            NSDate(),
            HKQueryOptionNone
        )

        return suspendCancellableCoroutine { continuation ->
            val query = HKSampleQuery(quantityType, predicate, 0u, null) { _, results, error ->
                if (error != null || results == null || results.isEmpty()) {
                    continuation.resume(0.0) {} // Return 0.0 or handle error case appropriately
                } else {
                    val sample = results.filterIsInstance<HKQuantitySample>().firstOrNull()
                    if (sample != null) {
                        continuation.resume(sample.quantity.doubleValueForUnit(unit)) {}
                    } else {
                        continuation.resume(0.0) {} // Return 0.0 or handle case where sample is null
                    }
                }
            }
            healthStore.executeQuery(query)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getQuantitySamples(identifier: String, unit: HKUnit): List<Double> {
        val quantityType = HKObjectType.quantityTypeForIdentifier(identifier)!!
        val predicate = HKQuery.predicateForSamplesWithStartDate(NSDate.distantPast, NSDate(), HKQueryOptionNone)

        return suspendCancellableCoroutine { continuation ->
            val query = HKSampleQuery(quantityType, predicate, 0u, null) { _, results, error ->
                if (error != null) {
                    continuation.resume(emptyList()) {}
                } else {
                    val samples = results?.filterIsInstance<HKQuantitySample>()?.map {
                        it.quantity.doubleValueForUnit(unit)
                    } ?: emptyList()
                    continuation.resume(samples) {}
                }
            }
            healthStore.executeQuery(query)
        }
    }
    @OptIn(InternalAPI::class)
    suspend fun sendHealthData(healthData: HealthData): HttpResponse {
        return client.post("http://192.168.1.90:8080/api/health/data") {
            contentType(ContentType.Application.Json)
            setBody(healthData)
        }
    }
}