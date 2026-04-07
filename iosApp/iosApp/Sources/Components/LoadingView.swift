import SwiftUI

struct LoadingView: View {
    var message: String = "Loading…"

    @State private var shimmerOffset: CGFloat = -200

    var body: some View {
        VStack(spacing: 12) {
            ForEach(0..<5, id: \.self) { _ in
                ShimmerRow(shimmerOffset: shimmerOffset)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .onAppear {
            withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                shimmerOffset = 400
            }
        }
    }
}

private struct ShimmerRow: View {
    let shimmerOffset: CGFloat

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(shimmerGradient)
                .frame(width: 52, height: 52)

            VStack(alignment: .leading, spacing: 8) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerGradient)
                    .frame(width: 140, height: 14)

                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerGradient)
                    .frame(width: 90, height: 10)
            }

            Spacer()
        }
        .padding(.vertical, 6)
    }

    private var shimmerGradient: LinearGradient {
        LinearGradient(
            colors: [
                Color(.systemGray5).opacity(0.6),
                Color(.systemGray5).opacity(0.2),
                Color(.systemGray5).opacity(0.6)
            ],
            startPoint: UnitPoint(x: shimmerOffset / 400, y: 0.5),
            endPoint: UnitPoint(x: (shimmerOffset + 200) / 400, y: 0.5)
        )
    }
}
