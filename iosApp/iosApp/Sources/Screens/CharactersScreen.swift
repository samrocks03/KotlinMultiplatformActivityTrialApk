import SwiftUI
import Shared

struct CharactersScreen: View {
    @StateObject private var viewModel = CharactersViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.characters.isEmpty {
                LoadingView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.characters.isEmpty {
                ErrorView(message: error) {
                    Task { await viewModel.refresh() }
                }
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            } else {
                List {
                    ForEach(viewModel.characters, id: \.id) { character in
                        NavigationLink(value: character.id) {
                            CharacterRow(character: character) {
                                Task { await viewModel.toggleFavorite(character: character) }
                            }
                        }
                        .onAppear {
                            Task { await viewModel.loadNextPageIfNeeded(currentItem: character) }
                        }
                    }

                    if viewModel.isLoading && !viewModel.characters.isEmpty {
                        HStack {
                            Spacer()
                            ProgressView()
                                .tint(NotionColors.accent)
                            Spacer()
                        }
                        .listRowSeparator(.hidden)
                    }
                }
                .listStyle(.plain)
                .refreshable {
                    await viewModel.refresh()
                }
                .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.isLoading)
        .animation(.easeInOut(duration: 0.3), value: viewModel.error == nil)
        .navigationTitle("Characters")
        .searchable(text: $viewModel.searchQuery, prompt: "Search characters…")
        .onSubmit(of: .search) {
            Task { await viewModel.search() }
        }
        .onChange(of: viewModel.searchQuery) { oldValue, newValue in
            if newValue.isEmpty && !oldValue.isEmpty {
                Task { await viewModel.search() }
            }
        }
        .navigationDestination(for: String.self) { characterId in
            CharacterDetailScreen(characterId: characterId)
        }
        .task {
            if viewModel.characters.isEmpty {
                await viewModel.loadCharacters()
            }
        }
    }
}
