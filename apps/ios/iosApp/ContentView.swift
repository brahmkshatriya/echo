import UIKit
import SwiftUI
import EchoApp

struct ComposeView: UIViewControllerRepresentable {
    let application: EchoApplication

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(application: application)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let application: EchoApplication

    var body: some View {
        ComposeView(application: application).ignoresSafeArea()
    }
}
