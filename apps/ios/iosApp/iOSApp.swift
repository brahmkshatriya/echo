import SwiftUI
import EchoApp

private final class ApplicationOwner: ObservableObject {
    let application = EchoApplication()

    deinit {
        application.close()
    }
}

@main
struct iOSApp: App {
    @StateObject private var owner = ApplicationOwner()

    var body: some Scene {
        WindowGroup {
            ContentView(application: owner.application)
        }
    }
}
