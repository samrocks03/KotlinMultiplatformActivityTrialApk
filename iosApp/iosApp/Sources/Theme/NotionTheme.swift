import SwiftUI

enum NotionColors {
    static let accent = Color(red: 0/255, green: 117/255, blue: 222/255)
    static let warmWhite = Color(red: 246/255, green: 245/255, blue: 244/255)
    static let warmDark = Color(red: 49/255, green: 48/255, blue: 46/255)
    static let textSecondary = Color(red: 97/255, green: 93/255, blue: 89/255)
    static let textTertiary = Color(red: 163/255, green: 158/255, blue: 152/255)
    static let whisperBorder = Color.primary.opacity(0.10)
    static let statusAlive = Color(red: 85/255, green: 204/255, blue: 68/255)
    static let statusDead = Color(red: 214/255, green: 61/255, blue: 46/255)
    static let badgeBg = Color(red: 242/255, green: 249/255, blue: 255/255)
    static let badgeText = Color(red: 9/255, green: 127/255, blue: 232/255)
}

struct NotionCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(NotionColors.whisperBorder, lineWidth: 1)
            )
    }
}

extension View {
    func notionCard() -> some View {
        modifier(NotionCardModifier())
    }
}
