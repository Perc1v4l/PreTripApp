// В iOS приложении (iosApp/iosApp/ContentView.swift)
import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack {
            Button(action: {
                let viewController = HealthViewController()
                viewController.sendDataButtonPressed(UIButton())
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
