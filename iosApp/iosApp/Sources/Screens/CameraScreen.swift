import SwiftUI

struct CameraScreen: View {
    @StateObject private var viewModel = CameraViewModel()
    @State private var pulseScale: CGFloat = 1.0

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Preview area
                if let image = viewModel.capturedImage {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxHeight: 300)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(NotionColors.whisperBorder, lineWidth: 1)
                        )
                        .transition(.scale(scale: 0.9).combined(with: .opacity))
                } else {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(NotionColors.warmWhite)
                        .frame(height: 220)
                        .overlay(
                            VStack(spacing: 12) {
                                Image(systemName: "camera")
                                    .font(.system(size: 40))
                                    .foregroundColor(NotionColors.textTertiary)
                                Text("No photo captured")
                                    .font(.subheadline)
                                    .foregroundColor(NotionColors.textSecondary)
                            }
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(NotionColors.whisperBorder, lineWidth: 1)
                        )
                }

                // Pulsing capture button
                ZStack {
                    // Pulse ring
                    Circle()
                        .stroke(NotionColors.accent.opacity(0.3), lineWidth: 2)
                        .frame(width: 56, height: 56)
                        .scaleEffect(pulseScale)
                        .opacity(2.0 - Double(pulseScale))

                    Button {
                        viewModel.isShowingPicker = true
                    } label: {
                        Label("Capture Photo", systemImage: "camera.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(NotionColors.accent)
                    .controlSize(.large)
                }
                .onAppear {
                    withAnimation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true)) {
                        pulseScale = 1.15
                    }
                }

                // Photo history
                if !viewModel.photos.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Photo History")
                            .font(.headline)

                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: 8) {
                            ForEach(viewModel.photos, id: \.absoluteString) { url in
                                if let data = try? Data(contentsOf: url),
                                   let uiImage = UIImage(data: data) {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(height: 100)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                        .transition(.scale.combined(with: .opacity))
                                }
                            }
                        }
                    }
                }
            }
            .padding(20)
            .animation(.spring(response: 0.4, dampingFraction: 0.8), value: viewModel.capturedImage != nil)
        }
        .navigationTitle("Camera")
        .sheet(isPresented: $viewModel.isShowingPicker) {
            ImagePicker { image in
                viewModel.onPhotoCaptured(image)
            }
        }
    }
}

// MARK: - ImagePicker UIViewControllerRepresentable

struct ImagePicker: UIViewControllerRepresentable {
    let onImagePicked: (UIImage) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onImagePicked: onImagePicked)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        if UIImagePickerController.isSourceTypeAvailable(.camera) {
            picker.sourceType = .camera
        } else {
            picker.sourceType = .photoLibrary
        }
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImagePicked: (UIImage) -> Void

        init(onImagePicked: @escaping (UIImage) -> Void) {
            self.onImagePicked = onImagePicked
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                onImagePicked(image)
            }
            picker.dismiss(animated: true)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            picker.dismiss(animated: true)
        }
    }
}
