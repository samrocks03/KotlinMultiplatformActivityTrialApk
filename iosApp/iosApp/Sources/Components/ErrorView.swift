import SwiftUI

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 44))
                .foregroundColor(NotionColors.textTertiary)

            Text(message)
                .font(.body)
                .foregroundColor(NotionColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button("Retry", action: onRetry)
                .buttonStyle(.borderedProminent)
                .tint(NotionColors.accent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
