import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.HealthKit.*
import platform.darwin.*
import platform.UIKit.UIDevice
import platform.HealthKit.HKUnit



data class HealthData(
    val heartRate: List<Double>,
    val bloodPressureSystolic: List<Double>,
    val bloodPressureDiastolic: List<Double>,
    val bodyTemperature: List<Double>,
    val bloodAlcoholContent: List<Double>,
    val deviceID: String
)

class HealthKitManager {
    private val healthStore: HKHealthStore = HKHealthStore()

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
            healthStore.requestAuthorizationToShareTypes(null, readTypes) { success, error ->
                if (success) {
                    continuation.resume(true) {}
                } else {
                    continuation.resume(false) {}
                }
            }
        }
    }

    suspend fun getHealthData(): HealthData {
        val heartRates = getQuantitySamples(HKQuantityTypeIdentifierHeartRate.toString(), HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit()))
        val bloodPressureSystolic = getQuantitySamples(HKQuantityTypeIdentifierBloodPressureSystolic.toString(), HKUnit.millimeterOfMercuryUnit())
        val bloodPressureDiastolic = getQuantitySamples(
            HKQuantityTypeIdentifierBloodPressureDiastolic.toString(), HKUnit.millimeterOfMercuryUnit())
        val bodyTemperature = getQuantitySamples(HKQuantityTypeIdentifierBodyTemperature.toString(), HKUnit.degreeCelsiusUnit())
        val bloodAlcoholContent = getQuantitySamples(HKQuantityTypeIdentifierBloodAlcoholContent.toString(), HKUnit.percentUnit())

        val deviceID = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: ""

        return HealthData(
            heartRate = heartRates,
            bloodPressureSystolic = bloodPressureSystolic,
            bloodPressureDiastolic = bloodPressureDiastolic,
            bodyTemperature = bodyTemperature,
            bloodAlcoholContent = bloodAlcoholContent,
            deviceID = deviceID
        )
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
}