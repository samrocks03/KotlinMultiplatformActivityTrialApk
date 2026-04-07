import SwiftUI
import Shared

struct FavoritesScreen: View {
    @StateObject private var viewModel = FavoritesViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.favorites.isEmpty {
                LoadingView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.favorites.isEmpty {
                ErrorView(message: error) {
                    Task { await viewModel.observeFavorites() }
                }
            } else if viewModel.favorites.isEmpty {
                ContentUnavailableView(
                    "No Favorites",
                    systemImage: "heart.slash",
                    description: Text("Characters you favorite will appear here.")
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            } else {
                List {
                    ForEach(viewModel.favorites, id: \.id) { character in
                        NavigationLink(value: character.id) {
                            CharacterRow(character: character) {
                                withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                    Task { await viewModel.removeFavorite(character: character) }
                                }
                            }
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                    Task { await viewModel.removeFavorite(character: character) }
                                }
                            } label: {
                                Label("Unfavorite", systemImage: "heart.slash")
                            }
                        }
                        .transition(.slide.combined(with: .opacity))
                    }
                }
                .listStyle(.plain)
                .animation(.spring(response: 0.4, dampingFraction: 0.8), value: viewModel.favorites.count)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.isLoading)
        .navigationTitle("Favorites")
        .navigationDestination(for: String.self) { characterId in
            CharacterDetailScreen(characterId: characterId)
        }
        .task {
            await viewModel.observeFavorites()
        }
    }
}
