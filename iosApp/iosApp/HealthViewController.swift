import UIKit
import HealthKit

class HealthViewController: UIViewController {
    let healthDataSender = HealthDataSender()
    let healthStore = HKHealthStore()

    override func viewDidLoad() {
        super.viewDidLoad()
        requestHealthKitPermissions()
        // Set up a timer to call readHealthData every hour
        Timer.scheduledTimer(timeInterval: 3600, target: self, selector: #selector(readHealthData), userInfo: nil, repeats: true)
    }

    func requestHealthKitPermissions() {
        guard HKHealthStore.isHealthDataAvailable() else {
            return
        }

        let typesToShare: Set = [
            HKObjectType.quantityType(forIdentifier: .bloodAlcoholContent)!,
            HKObjectType.quantityType(forIdentifier: .heartRate)!,
            HKObjectType.quantityType(forIdentifier: .bloodPressureSystolic)!,
            HKObjectType.quantityType(forIdentifier: .bloodPressureDiastolic)!
        ]

        healthStore.requestAuthorization(toShare: nil, read: typesToShare) { (success, error) in
            if !success {
                // Handle error
            }
        }
    }

    @IBAction func sendDataButtonPressed(_ sender: UIButton) {
        readHealthData()
    }

    func readHealthData() {
        let typesToRead = [
            HKQuantityType.quantityType(forIdentifier: .bloodAlcoholContent),
            HKQuantityType.quantityType(forIdentifier: .heartRate),
            HKQuantityType.quantityType(forIdentifier: .bloodPressureSystolic),
            HKQuantityType.quantityType(forIdentifier: .bloodPressureDiastolic)
        ].compactMap { $0 }

        let group = DispatchGroup()
        var healthData: [String: Any] = [:]

        for type in typesToRead {
            group.enter()
            readMostRecentSample(for: type) { (sample, error) in
                if let sample = sample {
                    let value = sample.quantity.doubleValue(for: HKUnit.count())
                    healthData[type.identifier] = value
                }
                group.leave()
            }
        }

        group.notify(queue: .main) {
            self.sendHealthDataToServer(data: healthData)
        }
    }

    func readMostRecentSample(for sampleType: HKSampleType, completion: @escaping (HKQuantitySample?, Error?) -> Void) {
        let mostRecentPredicate = HKQuery.predicateForSamples(withStart: Date.distantPast, end: Date(), options: .strictEndDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)
        let limit = 1

        let sampleQuery = HKSampleQuery(sampleType: sampleType, predicate: mostRecentPredicate, limit: limit, sortDescriptors: [sortDescriptor]) { (query, samples, error) in
            guard let samples = samples, let mostRecentSample = samples.first as? HKQuantitySample else {
                completion(nil, error)
                return
            }
            completion(mostRecentSample, nil)
        }
        healthStore.execute(sampleQuery)
    }

    func sendHealthDataToServer(data: [String: Any]) {
        guard let url = URL(string: "https://yourserver.com/api/healthdata") else {
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: data, options: [])
            request.httpBody = jsonData
        } catch {
            // Handle error
        }

        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("Error: \(error)")
                return
            }
            // Handle response if needed
        }
        task.resume()
    }
    

}
