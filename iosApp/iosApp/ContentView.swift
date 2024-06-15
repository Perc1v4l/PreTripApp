import SwiftUI
import shared

struct ContentView: View {
    @State private var message = ""
    var healthKitManager = HealthKitManager()
    @State private var healthData: HealthData? = nil
    
    var body: some View {
        VStack {
            Text("Welcome to Health App")
            if let healthData = healthData {
                Text("Pulse: \(healthData.pulse)")
                Text("Systolic Blood Pressure: \(healthData.blood_pressure_systolic)")
                Text("Diastolic Blood Pressure: \(healthData.blood_pressure_diastolic)")
                Text("Body Temperature: \(healthData.temperature)")
                Text("Blood Alcohol Content: \(healthData.blood_alcohol_level)")
                Text("Device ID: \(healthData.idfv)")
            }
            Button(action: {
                message = ""
                Task {
                    if healthKitManager.isHealthDataAvailable() {
                        do {
                            let isAuthorized = try await healthKitManager.requestAuthorization()
                            if isAuthorized as! Bool {
                                let healthData = try await healthKitManager.getHealthData()
                                self.healthData = healthData
                                let response = try await healthKitManager.sendHealthData(healthData: healthData)
                                message = "Data sent successfully: \(response.status)"
                            } else {
                                message = "Authorization failed"
                            }
                        } catch {
                            message = "An error occurred: \(error.localizedDescription)"
                        }                       }
                }
            }) {
                Text("Send Health Data")
            }
            Text(message)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
