import SwiftUI

@MainActor
final class AppState: ObservableObject {
    @Published var isScanning = true
    @Published var scannedReference = ""
    @Published var item: Item?
    @Published var freezers: [Freezer] = []
    @Published var items: [Item] = []
    @Published var errorMessage: String?
    @Published var isLoading = false

#if DEBUG
    @AppStorage("apiBaseUrl") private var apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#else
    private let apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#endif
    @AppStorage("apiKey") private var apiKey: String = ""
    @AppStorage("apiKeyExpiry") private var apiKeyExpiry: String = ""

    var client: APIClient {
        APIClient(baseURL: apiBaseUrl, apiKey: apiKey, apiKeyExpiry: parseApiKeyExpiry(apiKeyExpiry))
    }

    func loadSummary() async {
        do {
            freezers = try await client.listFreezers()
            items = try await client.listItems().items
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func loadFreezers() async {
        do {
            freezers = try await client.listFreezers()
            if freezerIdDefault == 0, let first = freezers.first {
                freezerIdDefault = first.id
            }
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func handleScan(_ value: String) async {
        let reference = extractReference(from: value)
        scannedReference = reference
        isScanning = false
        await lookupItem(by: reference)
    }

    func lookupItem(by reference: String) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let list = try await client.listItems()
            items = list.items
            item = list.items.first { $0.reference == reference }
            if item == nil {
                await loadFreezers()
            }
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func createItem(payload: ItemCreatePayload) async {
        isLoading = true
        defer { isLoading = false }
        do {
            item = try await client.createItem(payload)
            await loadSummary()
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func updateItem(item: Item) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let payload = ItemCreatePayload(
                reference: item.reference,
                freezeDate: item.freezeDate,
                bestBefore: item.bestBefore,
                description: item.description,
                freezerId: item.freezerId,
                shelfNumber: item.shelfNumber,
                weight: item.weight,
                size: item.size
            )
            self.item = try await client.updateItem(id: item.id, payload: payload)
            await loadSummary()
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func deleteItem(id: UUID) async {
        isLoading = true
        defer { isLoading = false }
        do {
            try await client.deleteItem(id: id)
            item = nil
            await loadSummary()
        } catch {
            if !isCancellation(error) {
                errorMessage = error.localizedDescription
            }
        }
    }

    func reset() {
        isScanning = true
        scannedReference = ""
        item = nil
        errorMessage = nil
    }

    private func extractReference(from rawValue: String) -> String {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.hasPrefix("r:") {
            let parts = trimmed.split(separator: ",")
            if let referencePart = parts.first(where: { $0.starts(with: "r:") }) {
                return referencePart.replacingOccurrences(of: "r:", with: "")
            }
        }
        return trimmed
    }

    private func isCancellation(_ error: Error) -> Bool {
        if error is CancellationError {
            return true
        }
        if (error as NSError).code == URLError.cancelled.rawValue {
            return true
        }
        return false
    }

    @Published var freezerIdDefault = 0
}

struct ContentView: View {
    @StateObject private var state = AppState()
    @State private var description = ""
    @State private var freezerId = 0
    @State private var freezeDate = Date()
    @State private var bestBefore = Calendar.current.date(byAdding: .month, value: 6, to: Date()) ?? Date()
    @State private var shelfNumber: Double = 1
    @State private var weight = ""
    @State private var size = ""
    @State private var manualReference = ""
    @State private var selectedTab = 0
    @State private var now = Date()

    @AppStorage("apiKey") private var apiKey: String = ""
    @AppStorage("apiKeyExpiry") private var apiKeyExpiry: String = ""

    private var isSignedIn: Bool {
        guard let expiryDate = parseApiKeyExpiry(apiKeyExpiry) else {
            return false
        }
        return !apiKey.isEmpty && expiryDate > now
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationView {
                Group {
                    if isSignedIn {
                        ItemsSummaryView(freezers: state.freezers, items: state.items) { updated in
                            Task { await state.updateItem(item: updated) }
                        } onDelete: { item in
                            Task { await state.deleteItem(id: item.id) }
                        }
                        .refreshable {
                            await state.loadSummary()
                        }
                        .task {
                            await state.loadSummary()
                        }
                    } else {
                        LoginRequiredView {
                            selectedTab = 2
                        }
                    }
                }
                .navigationTitle("")
            }
            .tabItem {
                Label("Items", systemImage: "list.bullet")
            }
            .tag(0)

            NavigationView {
                Group {
                    if isSignedIn {
                        if state.isScanning {
                            VStack {
                                VStack(spacing: 16) {
                                    Text("Scan QR code")
                                        .font(.headline)
                                        .frame(maxWidth: .infinity, alignment: .center)
                                        .padding(.horizontal)

                                    QRScannerView { value in
                                        Task { await state.handleScan(value) }
                                    }
                                    .frame(height: 320)
                                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                                    VStack(alignment: .leading, spacing: 12) {
                                        Text("Enter reference number manually")
                                            .font(.headline)
                                        HStack(spacing: 12) {
                                            TextField("Reference", text: $manualReference)
                                                .textInputAutocapitalization(.never)
                                                .keyboardType(.numberPad)
                                                .textFieldStyle(.roundedBorder)
                                            Button("Use") {
                                                Task { await state.handleScan(manualReference) }
                                            }
                                            .disabled(manualReference.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                                        }
                                        Text("Reference will default to the next value in the sequence.")
                                            .font(.footnote)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                .padding()
                                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                                .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
                                .padding()
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                            .task(id: state.isScanning) {
                                await loadNextReference()
                            }
                        } else if state.isLoading {
                            ProgressView("Loading...")
                                .padding()
                        } else if let item = state.item {
                            ItemDetailView(item: item, freezers: state.freezers) { updated in
                                Task { await state.updateItem(item: updated) }
                            } onDelete: { item in
                                Task {
                                    await state.deleteItem(id: item.id)
                                    state.reset()
                                    resetNewItemDraft()
                                }
                            } onBack: {
                                state.reset()
                                manualReference = ""
                                resetNewItemDraft()
                            }
                        } else {
                            AddItemPanelView(
                                reference: $state.scannedReference,
                                description: $description,
                                freezerId: $freezerId,
                                freezeDate: $freezeDate,
                                bestBefore: $bestBefore,
                                shelfNumber: $shelfNumber,
                                weight: $weight,
                                size: $size,
                                freezers: state.freezers,
                                isDisabled: state.freezers.isEmpty,
                                isDoneDisabled: state.freezers.isEmpty ||
                                    description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                            ) {
                                state.reset()
                                manualReference = ""
                                resetNewItemDraft()
                            } onDone: {
                                Task {
                                    let payload = ItemCreatePayload(
                                        reference: state.scannedReference,
                                        freezeDate: formattedDate(freezeDate),
                                        bestBefore: formattedDate(bestBefore),
                                        description: description,
                                        freezerId: freezerId == 0 ? state.freezerIdDefault : freezerId,
                                        shelfNumber: Int(shelfNumber),
                                        weight: sanitisedWeight(weight),
                                        size: sanitisedSize(size)
                                    )
                                    await state.createItem(payload: payload)
                                }
                            }
                        }
                    } else {
                        LoginRequiredView {
                            selectedTab = 2
                        }
                    }
                }
                .navigationTitle("")
                .toolbar {}
                .alert(item: Binding(
                    get: {
                        state.errorMessage.map { MessageWrapper(message: $0) }
                    },
                    set: { _ in state.errorMessage = nil }
                )) { wrapper in
                    Alert(title: Text("Error"), message: Text(wrapper.message), dismissButton: .default(Text("OK")))
                }
                .task {
                    if isSignedIn {
                        await state.loadFreezers()
                    }
                }
            }
            .tabItem {
                Label("Scan", systemImage: "qrcode.viewfinder")
            }
            .tag(1)

            NavigationView {
                SettingsView()
            }
            .tabItem {
                Label("Settings", systemImage: "gearshape")
            }
            .tag(2)
        }
        .onAppear {
            clearExpiredKeyIfNeeded()
        }
        .onReceive(Timer.publish(every: 60, on: .main, in: .common).autoconnect()) { value in
            now = value
            clearExpiredKeyIfNeeded()
        }
        .onChange(of: isSignedIn) { _, newValue in
            if !newValue {
                state.reset()
                state.freezers = []
                state.items = []
                state.item = nil
            }
        }
    }

    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private func loadNextReference() async {
        guard state.isScanning, isSignedIn else { return }
        do {
            let response = try await state.client.nextReference()
            manualReference = response.nextReference
        } catch {
            if state.errorMessage == nil {
                state.errorMessage = error.localizedDescription
            }
        }
    }

    private func sanitisedWeight(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        return String(trimmed.prefix(8))
    }

    private func sanitisedSize(_ rawValue: String) -> String? {
        switch rawValue {
        case "S", "M", "L":
            return rawValue
        default:
            return nil
        }
    }

    private func clearExpiredKeyIfNeeded() {
        guard !apiKey.isEmpty else { return }
        guard let expiryDate = parseApiKeyExpiry(apiKeyExpiry) else {
            apiKey = ""
            apiKeyExpiry = ""
            return
        }
        if expiryDate <= now {
            apiKey = ""
            apiKeyExpiry = ""
        }
    }

    private func resetNewItemDraft() {
        description = ""
        weight = ""
        size = ""
        freezeDate = Date()
        bestBefore = Calendar.current.date(byAdding: .month, value: 6, to: freezeDate) ?? freezeDate
        shelfNumber = 1
    }
}

struct MessageWrapper: Identifiable {
    let id = UUID()
    let message: String
}

private struct LoginRequiredView: View {
    let onOpenSettings: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Sign in required")
                .font(.title3.weight(.semibold))
            Text("Open Settings to sign in and access freezer items.")
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button("Open Settings") {
                onOpenSettings()
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

private struct LiquidGlassButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.weight(.semibold))
            .foregroundColor(.primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(.ultraThinMaterial, in: Capsule())
            .overlay(
                Capsule()
                    .stroke(Color.white.opacity(0.35), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.18), radius: 12, x: 0, y: 6)
            .opacity(isEnabled ? (configuration.isPressed ? 0.85 : 1) : 0.5)
    }
}

private struct AddItemPanelView: View {
    @Binding var reference: String
    @Binding var description: String
    @Binding var freezerId: Int
    @Binding var freezeDate: Date
    @Binding var bestBefore: Date
    @Binding var shelfNumber: Double
    @Binding var weight: String
    @Binding var size: String
    let freezers: [Freezer]
    let isDisabled: Bool
    let isDoneDisabled: Bool
    let onBack: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack {
            VStack(spacing: 16) {
                ZStack {
                    Text("New item")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                    HStack {
                        Button {
                            onBack()
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .accessibilityLabel("Back")
                        Spacer()
                        Button("Done") {
                            onDone()
                        }
                        .disabled(isDoneDisabled)
                    }
                }
                .padding(.horizontal)

                ScrollView {
                    ItemFormView(
                        reference: $reference,
                        description: $description,
                        freezerId: $freezerId,
                        freezeDate: $freezeDate,
                        bestBefore: $bestBefore,
                        shelfNumber: $shelfNumber,
                        weight: $weight,
                        size: $size,
                        freezers: freezers,
                        isDisabled: isDisabled
                    ) {}
                }
            }
            .padding()
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
            .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}
