package com.example.pretripapp

import platform.Foundation.HTTPMethod
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse

actual class HealthDataSender {
    actual fun sendHealthData() {
        // Данные HealthKit должны быть переданы здесь
        val healthData: Map<String, Any> = mapOf(
            "heartRate" to 72, // Пример значений
            "bloodAlcoholContent" to 0.02,
            "bloodPressureSystolic" to 120,
            "bloodPressureDiastolic" to 80
        )

        sendHealthDataToServer(data = healthData)
    }

    private fun sendHealthDataToServer(data: Map<String, Any>) {
        val url = NSURL(string = "https://yourserver.com/api/healthdata")
        val request = NSMutableURLRequest(url = url!!)
        request.HTTPMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField = "Content-Type")

        val jsonData = NSJSONSerialization.dataWithJSONObject(data, 0, null)

        request.HTTPBody = jsonData

        val task: NSURLSessionDataTask = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            if (error != null) {
                NSLog("Error: \(error)")
            } else {
                NSLog("Success: \(response)")
            }
        }
        task.resume()
    }
}