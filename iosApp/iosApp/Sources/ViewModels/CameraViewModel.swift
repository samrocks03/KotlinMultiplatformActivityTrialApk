import SwiftUI
import Shared

@MainActor
class CameraViewModel: ObservableObject {
    @Published var capturedImage: UIImage?
    @Published var photos: [URL] = []
    @Published var isShowingPicker = false

    private let capturePhotoUseCase = KoinHelper.shared.resolve(CapturePhotoUseCase.self)

    init() {
        loadSavedPhotos()
    }

    func onPhotoCaptured(_ image: UIImage) {
        capturedImage = image

        guard let data = image.jpegData(compressionQuality: 0.8) else { return }
        let filename = "photo_\(Int(Date().timeIntervalSince1970)).jpg"
        let documentsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileURL = documentsDir.appendingPathComponent(filename)

        do {
            try data.write(to: fileURL)
            photos.insert(fileURL, at: 0)
        } catch {
            print("Failed to save photo: \(error)")
        }

        Task {
            do {
                let _ = try await capturePhotoUseCase.invoke(platformPath: fileURL.path)
            } catch {
                print("Failed to record photo in shared layer: \(error)")
            }
        }
    }

    private func loadSavedPhotos() {
        let documentsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        do {
            let files = try FileManager.default.contentsOfDirectory(at: documentsDir, includingPropertiesForKeys: [.creationDateKey], options: .skipsHiddenFiles)
            photos = files
                .filter { $0.pathExtension == "jpg" }
                .sorted { ($0.lastPathComponent) > ($1.lastPathComponent) }
        } catch {
            print("Failed to load photos: \(error)")
        }
    }
}
