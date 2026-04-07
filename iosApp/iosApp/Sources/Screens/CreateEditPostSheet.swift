import SwiftUI
import Shared

struct CreateEditPostSheet: View {
    enum Mode {
        case create
        case edit(post: Post)
    }

    let mode: Mode
    let onSave: (String, String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title: String = ""
    @State private var bodyText: String = ""

    init(mode: Mode, onSave: @escaping (String, String) -> Void) {
        self.mode = mode
        self.onSave = onSave

        switch mode {
        case .create:
            _title = State(initialValue: "")
            _bodyText = State(initialValue: "")
        case .edit(let post):
            _title = State(initialValue: post.title)
            _bodyText = State(initialValue: post.body)
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Title", text: $title)
                        .font(.headline)
                }

                Section {
                    TextEditor(text: $bodyText)
                        .frame(minHeight: 200)
                        .font(.body)
                }
            }
            .navigationTitle(isCreating ? "New Post" : "Edit Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(title, bodyText)
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                    .tint(NotionColors.accent)
                }
            }
        }
    }

    private var isCreating: Bool {
        if case .create = mode { return true }
        return false
    }
}
