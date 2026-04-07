import SwiftUI
import Shared

struct PostRow: View {
    let post: Post

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(post.title)
                .font(.headline)
                .lineLimit(1)

            Text(post.body)
                .font(.subheadline)
                .foregroundColor(NotionColors.textSecondary)
                .lineLimit(3)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .notionCard()
    }
}
