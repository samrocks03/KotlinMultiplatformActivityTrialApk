import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Characters", systemImage: "person.3.fill", value: 0) {
                NavigationStack { CharactersScreen() }
            }
            Tab("Posts", systemImage: "doc.text.fill", value: 1) {
                NavigationStack { PostsScreen() }
            }
            Tab("Camera", systemImage: "camera.fill", value: 2) {
                NavigationStack { CameraScreen() }
            }
            Tab("GPS", systemImage: "location.fill", value: 3) {
                NavigationStack { LocationScreen() }
            }
            Tab("Favorites", systemImage: "heart.fill", value: 4) {
                NavigationStack { FavoritesScreen() }
            }
        }
        .tint(NotionColors.accent)
    }
}
