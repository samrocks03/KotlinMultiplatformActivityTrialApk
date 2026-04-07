import SwiftUI
import Shared

@MainActor
class CharacterDetailViewModel: ObservableObject {
    @Published var detail: CharacterDetail?
    @Published var isLoading = false
    @Published var error: String?

    private let getDetailUseCase = KoinHelper.shared.resolve(GetCharacterDetailUseCase.self)
    private let toggleFavoriteUseCase = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)

    let characterId: String

    init(characterId: String) {
        self.characterId = characterId
    }

    func loadDetail() async {
        isLoading = true
        error = nil

        do {
            detail = try await getDetailUseCase.invoke(id: characterId)
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    func toggleFavorite() async {
        guard let detail = detail else { return }
        do {
            let _ = try await toggleFavoriteUseCase.invoke(characterId: detail.character.id)
            let c = detail.character
            let updatedCharacter = c.doCopy(
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
            self.detail = detail.doCopy(
                character: updatedCharacter,
                origin: detail.origin,
                location: detail.location,
                episodes: detail.episodes
            )
        } catch {
            self.error = error.localizedDescription
        }
    }
}
