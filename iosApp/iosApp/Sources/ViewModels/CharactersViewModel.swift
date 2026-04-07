import SwiftUI
import Shared

@MainActor
class CharactersViewModel: ObservableObject {
    @Published var characters: [Character_] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var searchQuery = ""
    @Published var hasMorePages = true

    private let getCharactersUseCase = KoinHelper.shared.resolve(GetCharactersUseCase.self)
    private let toggleFavoriteUseCase = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)
    private var currentPage: Int32 = 1

    func loadCharacters(reset: Bool = false) async {
        if reset {
            currentPage = 1
            characters = []
            hasMorePages = true
        }

        guard hasMorePages, !isLoading else { return }
        isLoading = true
        error = nil

        do {
            let page = try await getCharactersUseCase.invoke(
                page: currentPage,
                name: searchQuery.isEmpty ? nil : searchQuery
            )
            let newCharacters = page.results as? [Character_] ?? []
            characters.append(contentsOf: newCharacters)
            hasMorePages = page.info.next != nil
            currentPage += 1
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    func search() async {
        await loadCharacters(reset: true)
    }

    func loadNextPageIfNeeded(currentItem: Character_) async {
        guard let lastItem = characters.last,
              lastItem.id == currentItem.id else { return }
        await loadCharacters()
    }

    func toggleFavorite(character: Character_) async {
        do {
            let _ = try await toggleFavoriteUseCase.invoke(characterId: character.id)
            if let index = characters.firstIndex(where: { $0.id == character.id }) {
                let c = characters[index]
                let updated = c.doCopy(
                    id: c.id,
                    name: c.name,
                    status: c.status,
                    species: c.species,
                    gender: c.gender,
                    origin: c.origin,
                    location: c.location,
                    imageUrl: c.imageUrl,
                    episodeIds: c.episodeIds,
                    isFavorite: KotlinBoolean(bool: !c.isFavorite.boolValue)
                )
                characters[index] = updated
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func refresh() async {
        await loadCharacters(reset: true)
    }
}
