import SwiftUI

struct FreezerSetupView: View {
    @ObservedObject var appState: AppState

#if DEBUG
    @AppStorage("apiBaseUrl") private var apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#else
    private let apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#endif
    @AppStorage("apiKey") private var apiKey: String = ""
    @AppStorage("apiKeyExpiry") private var apiKeyExpiry: String = ""

    @State private var freezers: [Freezer] = []
    @State private var isLoading = false
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var editor: FreezerEditorState?
    @State private var freezerPendingDelete: Freezer?

    private var client: APIClient {
        APIClient(baseURL: apiBaseUrl, apiKey: apiKey, apiKeyExpiry: parseApiKeyExpiry(apiKeyExpiry))
    }

    var body: some View {
        List {
            Section {
                Button {
                    editor = .create
                } label: {
                    Label("Add freezer", systemImage: "plus")
                }
            }

            Section("Existing freezers") {
                if isLoading {
                    ProgressView("Loading freezers...")
                        .frame(maxWidth: .infinity, alignment: .center)
                } else if freezers.isEmpty {
                    Text("No freezers added yet.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(freezers) { freezer in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(freezer.name)
                                        .font(.headline)
                                    Text("\(freezer.shelfCount) shelves")
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button("Edit") {
                                    editor = .edit(freezer)
                                }
                                .buttonStyle(.bordered)
                            }

                            Button("Delete", role: .destructive) {
                                freezerPendingDelete = freezer
                            }
                            .buttonStyle(.borderless)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .navigationTitle("Setup freezers")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable {
            await loadFreezers()
        }
        .task {
            await loadFreezers()
        }
        .sheet(item: $editor) { state in
            FreezerEditorSheet(
                title: state.title,
                initialName: state.name,
                initialShelfCount: state.shelfCount,
                isSaving: isSaving
            ) { name, shelfCount in
                await saveFreezer(state: state, name: name, shelfCount: shelfCount)
            }
        }
        .alert("Delete freezer?", isPresented: Binding(
            get: { freezerPendingDelete != nil },
            set: { newValue in
                if !newValue {
                    freezerPendingDelete = nil
                }
            }
        ), presenting: freezerPendingDelete) { freezer in
            Button("Delete", role: .destructive) {
                Task { await deleteFreezer(freezer) }
            }
            Button("Cancel", role: .cancel) {}
        } message: { freezer in
            Text("Delete \(freezer.name)? This only works if no items are assigned to it.")
        }
        .alert(item: Binding(
            get: {
                errorMessage.map { FreezerSetupMessage(message: $0) }
            },
            set: { _ in errorMessage = nil }
        )) { wrapper in
            Alert(title: Text("Error"), message: Text(wrapper.message), dismissButton: .default(Text("OK")))
        }
    }

    private func loadFreezers() async {
        isLoading = true
        defer { isLoading = false }

        do {
            freezers = try await client.listFreezers().sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        } catch {
            if !isCancellationError(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func saveFreezer(state: FreezerEditorState, name: String, shelfCount: Int) async {
        isSaving = true
        defer { isSaving = false }

        do {
            let payload = FreezerPayload(name: name, shelfCount: shelfCount)
            switch state {
            case .create:
                _ = try await client.createFreezer(payload)
            case .edit(let freezer):
                _ = try await client.updateFreezer(id: freezer.id, payload: payload)
            }
            editor = nil
            await refreshAppState()
        } catch {
            if !isCancellationError(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func deleteFreezer(_ freezer: Freezer) async {
        freezerPendingDelete = nil
        isSaving = true
        defer { isSaving = false }

        do {
            let items = try await client.listAllItems()
            if items.contains(where: { $0.freezerId == freezer.id }) {
                errorMessage = "Cannot delete \(freezer.name). Move items out of this freezer first."
                return
            }

            try await client.deleteFreezer(id: freezer.id)
            await refreshAppState()
        } catch {
            if !isCancellationError(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func refreshAppState() async {
        await loadFreezers()
        await appState.loadSummary()
    }
}

private struct FreezerEditorSheet: View {
    let title: String
    let initialName: String
    let initialShelfCount: Int
    let isSaving: Bool
    let onSave: (String, Int) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var shelfCount: Int
    @State private var validationMessage: String?

    init(
        title: String,
        initialName: String,
        initialShelfCount: Int,
        isSaving: Bool,
        onSave: @escaping (String, Int) async -> Void
    ) {
        self.title = title
        self.initialName = initialName
        self.initialShelfCount = initialShelfCount
        self.isSaving = isSaving
        self.onSave = onSave
        _name = State(initialValue: initialName)
        _shelfCount = State(initialValue: initialShelfCount)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Freezer name", text: $name)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()

                    Stepper(value: $shelfCount, in: 1...10) {
                        LabeledContent("Shelves", value: "\(shelfCount)")
                    }
                }

                if let validationMessage {
                    Section {
                        Text(validationMessage)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .disabled(isSaving)
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Saving..." : "Save") {
                        Task {
                            guard let payload = validatedPayload() else { return }
                            await onSave(payload.name, payload.shelfCount)
                        }
                    }
                    .disabled(isSaving)
                }
            }
        }
    }

    private func validatedPayload() -> (name: String, shelfCount: Int)? {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedName.isEmpty else {
            validationMessage = "Freezer name is required."
            return nil
        }

        guard trimmedName.count <= 255 else {
            validationMessage = "Freezer name must be 255 characters or fewer."
            return nil
        }

        guard !containsUnsafeChars(trimmedName) else {
            validationMessage = "Freezer name contains invalid characters."
            return nil
        }

        validationMessage = nil
        return (trimmedName, shelfCount)
    }

    private func containsUnsafeChars(_ value: String) -> Bool {
        let normalised = value.lowercased()
        if normalised.contains("javascript:") || normalised.contains("data:text/html") {
            return true
        }

        for scalar in value.unicodeScalars {
            switch scalar {
            case "\u{003C}", "\u{003E}", "\u{0022}", "\u{0027}", "\u{0060}":
                return true
            default:
                if CharacterSet.controlCharacters.contains(scalar)
                    && scalar.value != 0x0A
                    && scalar.value != 0x0D
                    && scalar.value != 0x09 {
                    return true
                }
            }
        }

        return false
    }
}

private enum FreezerEditorState: Identifiable {
    case create
    case edit(Freezer)

    var id: String {
        switch self {
        case .create:
            return "create"
        case .edit(let freezer):
            return "edit-\(freezer.id)"
        }
    }

    var title: String {
        switch self {
        case .create:
            return "Add freezer"
        case .edit:
            return "Edit freezer"
        }
    }

    var name: String {
        switch self {
        case .create:
            return ""
        case .edit(let freezer):
            return freezer.name
        }
    }

    var shelfCount: Int {
        switch self {
        case .create:
            return 5
        case .edit(let freezer):
            return freezer.shelfCount
        }
    }
}

private struct FreezerSetupMessage: Identifiable {
    let id = UUID()
    let message: String
}
