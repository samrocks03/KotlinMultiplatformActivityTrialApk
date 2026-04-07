import SwiftUI
import Shared

struct PostsScreen: View {
    @StateObject private var viewModel = PostsViewModel()
    @State private var showCreateSheet = false
    @State private var editingPost: Post?

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.posts.isEmpty {
                LoadingView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.posts.isEmpty {
                ErrorView(message: error) {
                    Task { await viewModel.refresh() }
                }
            } else if viewModel.posts.isEmpty {
                ContentUnavailableView(
                    "No Posts",
                    systemImage: "doc.text",
                    description: Text("Tap + to create your first post.")
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            } else {
                List {
                    ForEach(viewModel.posts, id: \.id) { post in
                        PostRow(post: post)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                            .onTapGesture {
                                editingPost = post
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                        Task { await viewModel.deletePost(id: post.id) }
                                    }
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                            .transition(.slide.combined(with: .opacity))
                    }
                }
                .listStyle(.plain)
                .refreshable {
                    await viewModel.refresh()
                }
                .animation(.spring(response: 0.4, dampingFraction: 0.8), value: viewModel.posts.count)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.isLoading)
        .navigationTitle("Posts")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreateSheet = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundColor(NotionColors.accent)
                }
            }
        }
        .sheet(isPresented: $showCreateSheet) {
            CreateEditPostSheet(mode: .create) { title, bodyText in
                Task { await viewModel.createPost(title: title, body: bodyText) }
            }
        }
        .sheet(item: $editingPost) { post in
            CreateEditPostSheet(mode: .edit(post: post)) { title, bodyText in
                Task { await viewModel.updatePost(id: post.id, title: title, body: bodyText) }
            }
        }
        .task {
            if viewModel.posts.isEmpty {
                await viewModel.loadPosts()
            }
        }
    }
}
