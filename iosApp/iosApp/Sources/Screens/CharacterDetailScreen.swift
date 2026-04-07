import SwiftUI
import Shared

struct CharacterDetailScreen: View {
    let characterId: String
    @StateObject private var viewModel: CharacterDetailViewModel
    @State private var heroVisible = false
    @State private var contentVisible = false

    init(characterId: String) {
        self.characterId = characterId
        _viewModel = StateObject(wrappedValue: CharacterDetailViewModel(characterId: characterId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingView()
            } else if let error = viewModel.error {
                ErrorView(message: error) {
                    Task { await viewModel.loadDetail() }
                }
            } else if let detail = viewModel.detail {
                detailContent(detail)
            }
        }
        .navigationTitle(viewModel.detail?.character.name ?? "Detail")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if let detail = viewModel.detail {
                    Button {
                        Task { await viewModel.toggleFavorite() }
                    } label: {
                        Image(systemName: detail.character.isFavorite.boolValue ? "heart.fill" : "heart")
                            .foregroundColor(detail.character.isFavorite.boolValue ? .red : NotionColors.textTertiary)
                            .symbolEffect(.bounce, value: detail.character.isFavorite.boolValue)
                    }
                }
            }
        }
        .task {
            await viewModel.loadDetail()
        }
    }

    @ViewBuilder
    private func detailContent(_ detail: CharacterDetail) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                // Hero image with fade + scale entry
                AsyncImage(url: URL(string: detail.character.imageUrl)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .scaleEffect(heroVisible ? 1.0 : 1.05)
                            .opacity(heroVisible ? 1 : 0)
                            .animation(.spring(response: 0.5, dampingFraction: 0.8), value: heroVisible)
                            .onAppear {
                                withAnimation {
                                    heroVisible = true
                                }
                            }
                    case .failure:
                        Color(NotionColors.warmWhite)
                            .overlay(
                                Image(systemName: "person.crop.circle")
                                    .font(.system(size: 60))
                                    .foregroundColor(NotionColors.textTertiary)
                            )
                    case .empty:
                        Color(NotionColors.warmWhite)
                            .overlay(ProgressView().tint(NotionColors.accent))
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(height: 300)
                .clipped()

                // Content slides up
                VStack(alignment: .leading, spacing: 20) {
                    HStack {
                        Text(detail.character.name)
                            .font(.title.bold())

                        Spacer()

                        statusBadge(detail.character.status)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        infoRow(label: "Species", value: detail.character.species)
                        infoRow(label: "Gender", value: detail.character.gender)
                    }
                    .padding(16)
                    .notionCard()

                    if let origin = detail.origin {
                        sectionHeader("Origin")
                        VStack(alignment: .leading, spacing: 6) {
                            Text(origin.name)
                                .font(.body)
                            if !origin.type.isEmpty {
                                Text(origin.type)
                                    .font(.subheadline)
                                    .foregroundColor(NotionColors.textSecondary)
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .notionCard()
                    }

                    if let location = detail.location {
                        sectionHeader("Last Known Location")
                        VStack(alignment: .leading, spacing: 6) {
                            Text(location.name)
                                .font(.body)
                            if !location.type.isEmpty {
                                Text(location.type)
                                    .font(.subheadline)
                                    .foregroundColor(NotionColors.textSecondary)
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .notionCard()
                    }

                    let episodes = detail.episodes as? [Episode] ?? []
                    if !episodes.isEmpty {
                        sectionHeader("Episodes (\(episodes.count))")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(episodes, id: \.id) { episode in
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(episode.episode)
                                            .font(.caption.bold())
                                            .foregroundColor(NotionColors.badgeText)
                                        Text(episode.name)
                                            .font(.caption)
                                            .lineLimit(1)
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(NotionColors.badgeBg)
                                    .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }
                .padding(20)
                .offset(y: contentVisible ? 0 : 30)
                .opacity(contentVisible ? 1 : 0)
                .animation(.spring(response: 0.5, dampingFraction: 0.85).delay(0.15), value: contentVisible)
                .onAppear {
                    contentVisible = true
                }
            }
        }
    }

    private func statusBadge(_ status: CharacterStatus) -> some View {
        HStack(spacing: 6) {
            Circle()
                .fill(statusColor(for: status))
                .frame(width: 8, height: 8)
            Text(status.name)
                .font(.subheadline.weight(.medium))
                .foregroundColor(NotionColors.badgeText)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(NotionColors.badgeBg)
        .clipShape(Capsule())
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(NotionColors.textSecondary)
                .frame(width: 80, alignment: .leading)
            Text(value)
                .font(.subheadline)
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.headline)
            .padding(.top, 4)
    }

    private func statusColor(for status: CharacterStatus) -> Color {
        switch status {
        case .alive: return NotionColors.statusAlive
        case .dead: return NotionColors.statusDead
        default: return .gray
        }
    }
}
