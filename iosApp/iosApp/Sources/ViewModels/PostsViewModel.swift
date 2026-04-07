import SwiftUI
import Shared

extension Post: @retroactive Identifiable {}

@MainActor
class PostsViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var error: String?

    private let getPostsUseCase = KoinHelper.shared.resolve(GetPostsUseCase.self)
    private let createPostUseCase = KoinHelper.shared.resolve(CreatePostUseCase.self)
    private let updatePostUseCase = KoinHelper.shared.resolve(UpdatePostUseCase.self)
    private let deletePostUseCase = KoinHelper.shared.resolve(DeletePostUseCase.self)

    private var currentPage: Int32 = 1
    @Published var hasMorePages = true

    func loadPosts(reset: Bool = false) async {
        if reset {
            currentPage = 1
            posts = []
            hasMorePages = true
        }

        guard hasMorePages, !isLoading else { return }
        isLoading = true
        error = nil

        do {
            let page = try await getPostsUseCase.invoke(page: currentPage)
            let newPosts = page.posts as? [Post] ?? []
            posts.append(contentsOf: newPosts)
            hasMorePages = page.hasMore.boolValue
            currentPage += 1
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    func createPost(title: String, body: String) async {
        do {
            let post = try await createPostUseCase.invoke(title: title, body: body)
            posts.insert(post, at: 0)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func updatePost(id: Int32, title: String, body: String) async {
        do {
            let updated = try await updatePostUseCase.invoke(id: id, title: title, body: body)
            if let index = posts.firstIndex(where: { $0.id == id }) {
                posts[index] = updated
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deletePost(id: Int32) async {
        do {
            let _ = try await deletePostUseCase.invoke(id: id)
            posts.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func refresh() async {
        await loadPosts(reset: true)
    }
}
