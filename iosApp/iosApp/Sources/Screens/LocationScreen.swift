import SwiftUI
import CoreLocation

struct LocationScreen: View {
    @StateObject private var viewModel = LocationViewModel()
    @State private var coordinatesVisible = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                if viewModel.permissionStatus == .notDetermined {
                    permissionRequestView
                } else if viewModel.permissionStatus == .denied || viewModel.permissionStatus == .restricted {
                    permissionDeniedView
                } else {
                    locationContentView
                }
            }
            .padding(20)
        }
        .navigationTitle("GPS Location")
    }

    private var permissionRequestView: some View {
        VStack(spacing: 20) {
            Image(systemName: "location.circle")
                .font(.system(size: 60))
                .foregroundColor(NotionColors.accent)
                .symbolEffect(.pulse)

            Text("Location Access")
                .font(.title2.bold())

            Text("Grant location permission to see your current coordinates.")
                .font(.body)
                .foregroundColor(NotionColors.textSecondary)
                .multilineTextAlignment(.center)

            Button("Enable Location") {
                viewModel.requestPermission()
            }
            .buttonStyle(.borderedProminent)
            .tint(NotionColors.accent)
            .controlSize(.large)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }

    private var permissionDeniedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "location.slash")
                .font(.system(size: 48))
                .foregroundColor(NotionColors.textTertiary)

            Text("Location Access Denied")
                .font(.headline)

            Text("Please enable location access in Settings to use this feature.")
                .font(.subheadline)
                .foregroundColor(NotionColors.textSecondary)
                .multilineTextAlignment(.center)

            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(NotionColors.accent)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }

    private var locationContentView: some View {
        VStack(spacing: 20) {
            GroupBox {
                VStack(spacing: 16) {
                    HStack {
                        Image(systemName: "location.fill")
                            .foregroundColor(NotionColors.accent)
                            .font(.title2)
                        Text("Coordinates")
                            .font(.headline)
                        Spacer()
                    }

                    if let lat = viewModel.latitude, let lon = viewModel.longitude {
                        VStack(spacing: 10) {
                            coordinateRow(label: "Latitude", value: String(format: "%.6f", lat))
                            coordinateRow(label: "Longitude", value: String(format: "%.6f", lon))
                        }
                        .opacity(coordinatesVisible ? 1 : 0)
                        .offset(y: coordinatesVisible ? 0 : 10)
                        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: coordinatesVisible)
                        .onAppear { coordinatesVisible = true }
                        .onChange(of: viewModel.latitude) { _, _ in
                            coordinatesVisible = false
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.8).delay(0.1)) {
                                coordinatesVisible = true
                            }
                        }
                    } else {
                        Text("Tap refresh to get your location")
                            .font(.subheadline)
                            .foregroundColor(NotionColors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }

                    if let lastUpdated = viewModel.lastUpdated {
                        Text("Updated \(lastUpdated.formatted(.relative(presentation: .named)))")
                            .font(.caption)
                            .foregroundColor(NotionColors.textTertiary)
                    }
                }
            }
            .backgroundStyle(Color(.systemBackground))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(NotionColors.whisperBorder, lineWidth: 1)
            )

            Button {
                Task { await viewModel.fetchLocation() }
            } label: {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                        .frame(maxWidth: .infinity)
                } else {
                    Label("Refresh Location", systemImage: "arrow.clockwise")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(NotionColors.accent)
            .controlSize(.large)
            .disabled(viewModel.isLoading)

            if let error = viewModel.error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(NotionColors.statusDead)
                    .multilineTextAlignment(.center)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.isLoading)
    }

    private func coordinateRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(NotionColors.textSecondary)
                .frame(width: 80, alignment: .leading)
            Text(value)
                .font(.system(.body, design: .monospaced))
            Spacer()
        }
    }
}
