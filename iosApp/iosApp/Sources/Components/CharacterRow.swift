import SwiftUI
import Shared

struct CharacterRow: View {
    let character: Character_
    let onToggleFavorite: () -> Void

    @State private var isPressed = false
    @State private var heartBounce = false

    var body: some View {
        HStack(spacing: 14) {
            AsyncImage(url: URL(string: character.imageUrl)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    Image(systemName: "person.crop.circle.badge.exclamationmark")
                        .foregroundColor(NotionColors.textTertiary)
                case .empty:
                    ProgressView()
                        .tint(NotionColors.accent)
                @unknown default:
                    EmptyView()
                }
            }
            .frame(width: 52, height: 52)
            .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(character.name)
                    .font(.headline)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Circle()
                        .fill(statusColor(for: character.status))
                        .frame(width: 8, height: 8)

                    Text(character.status.name)
                        .font(.subheadline)
                        .foregroundColor(NotionColors.textSecondary)

                    Text("·")
                        .foregroundColor(NotionColors.textTertiary)

                    Text(character.species)
                        .font(.subheadline)
                        .foregroundColor(NotionColors.textTertiary)
                        .lineLimit(1)
                }
            }

            Spacer()

            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.5)) {
                    heartBounce = true
                }
                onToggleFavorite()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    heartBounce = false
                }
            } label: {
                Image(systemName: character.isFavorite.boolValue ? "heart.fill" : "heart")
                    .foregroundColor(character.isFavorite.boolValue ? .red : NotionColors.textTertiary)
                    .font(.title3)
                    .scaleEffect(heartBounce ? 1.3 : 1.0)
                    .animation(.spring(response: 0.35, dampingFraction: 0.5), value: heartBounce)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 6)
        .scaleEffect(isPressed ? 0.97 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }

    private func statusColor(for status: CharacterStatus) -> Color {
        switch status {
        case .alive: return NotionColors.statusAlive
        case .dead: return NotionColors.statusDead
        default: return .gray
        }
    }
}
