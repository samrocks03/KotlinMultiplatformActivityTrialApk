import SwiftUI
import Shared

@MainActor
class FavoritesViewModel: ObservableObject {
    @Published var favorites: [Character_] = []
    @Published var isLoading = false
    @Published var error: String?

    private let getFavoritesUseCase = KoinHelper.shared.resolve(GetFavoritesUseCase.self)
    private let toggleFavoriteUseCase = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)

    func observeFavorites() async {
        isLoading = true
        do {
            for try await list in getFavoritesUseCase.invoke() {
                let chars = list as? [Character_] ?? []
                favorites = chars
                isLoading = false
            }
        } catch {
            self.error = error.localizedDescription
            isLoading = false
        }
    }

    func removeFavorite(character: Character_) async {
        do {
            let _ = try await toggleFavoriteUseCase.invoke(characterId: character.id)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
