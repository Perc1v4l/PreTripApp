import SwiftUI
import shared

struct ContentView: View {
//     let greet = Greeting().greet()
//
//     var body: some View {
//         Text(greet)

//var healthKitManager = HealthKitManager()
//    var networkClient = NetworkClient()
//
//    var body: some View {
//        VStack {
//            Text("Welcome to Health App")
//            Button(action: {
//                Task {
//                    if healthKitManager.isHealthDataAvailable() {
//                        let isAuthorized = await healthKitManager.requestAuthorization()
//                        if isAuthorized {
//                            let healthData = await healthKitManager.getHealthData()
//                            let response = await networkClient.sendHealthData(healthData: healthData)
//                            print("Data sent: \(response)")
//                        }
//                    }
//
//                }
//            }) {
//                Text("Send Health Data")
//            }
//        }
//    }
    
     var healthKitManager = HealthKitManager()
       var networkClient = NetworkClient()

      var body: some View {
          VStack {
              Text("Welcome to Health App")
              Button(action: {
                  Task {
                      do {
                          if healthKitManager.isHealthDataAvailable() {
                              let isAuthorized = try await healthKitManager.requestAuthorization()
                              if isAuthorized as! Bool {
                                  let healthData = try await healthKitManager.getHealthData()
                                  let response = try await networkClient.sendHealthData(healthData: healthData)
                                  print("Data sent: \(response)")
                              }
                          }
                      } catch {
                          print("Error: \(error)")
                      }
                  }
              }) {
                  Text("Send Health Data")
              }
          }
      }
  }

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
