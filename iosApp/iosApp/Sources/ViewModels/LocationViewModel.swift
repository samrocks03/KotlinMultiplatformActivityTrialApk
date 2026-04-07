import SwiftUI
import CoreLocation
import Shared

@MainActor
class LocationViewModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var latitude: Double?
    @Published var longitude: Double?
    @Published var lastUpdated: Date?
    @Published var isLoading = false
    @Published var error: String?
    @Published var permissionStatus: CLAuthorizationStatus = .notDetermined

    private let locationManager = CLLocationManager()
    private let getLocationUseCase = KoinHelper.shared.resolve(GetCurrentLocationUseCase.self)

    override init() {
        super.init()
        locationManager.delegate = self
        permissionStatus = locationManager.authorizationStatus
    }

    func requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }

    func fetchLocation() async {
        isLoading = true
        error = nil

        do {
            let location = try await getLocationUseCase.invoke()
            latitude = location.latitude
            longitude = location.longitude
            lastUpdated = Date()
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            permissionStatus = status
        }
    }
}
