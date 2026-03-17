import SwiftUI

struct ItemDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @State var item: Item
    let freezers: [Freezer]
    let onSave: (Item) -> Void
    let onDelete: (Item) -> Void
    let onBack: (() -> Void)?

    @State private var description: String
    @State private var freezerId: Int
    @State private var freezeDate: Date
    @State private var bestBefore: Date
    @State private var shelfNumber: Double
    @State private var weight: String
    @State private var size: String
    @State private var original: Item
    @State private var saveTask: Task<Void, Never>?
    @State private var showSavedToast = false
    @State private var showDeleteConfirm = false
    @State private var descriptionRevision = 0

    init(item: Item, freezers: [Freezer], onSave: @escaping (Item) -> Void, onDelete: @escaping (Item) -> Void, onBack: (() -> Void)? = nil) {
        self._item = State(initialValue: item)
        self.freezers = freezers
        self.onSave = onSave
        self.onDelete = onDelete
        self.onBack = onBack
        _original = State(initialValue: item)
        _description = State(initialValue: item.description)
        _freezerId = State(initialValue: item.freezerId)
        _freezeDate = State(initialValue: item.freezeDate.toDate())
        _bestBefore = State(initialValue: item.bestBefore.toDate())
        _shelfNumber = State(initialValue: Double(item.shelfNumber))
        _weight = State(initialValue: item.weight ?? "")
        _size = State(initialValue: item.size ?? "")
    }

    private var maxShelf: Int {
        let shelfCount = freezers.first(where: { $0.id == freezerId })?.shelfCount ?? 10
        return max(1, shelfCount)
    }

    private var isDirty: Bool {
        description != original.description ||
        freezerId != original.freezerId ||
        freezeDate.toApiString() != original.freezeDate ||
        bestBefore.toApiString() != original.bestBefore ||
        Int(shelfNumber) != original.shelfNumber ||
        weight.trimmingCharacters(in: .whitespacesAndNewlines) != (original.weight ?? "") ||
        size != (original.size ?? "")
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ZStack {
                    Text(item.reference)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                    HStack {
                        Button {
                            if let onBack {
                                onBack()
                            } else {
                                dismiss()
                            }
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .accessibilityLabel("Back")
                        Spacer()
                        Button("Delete") {
                            showDeleteConfirm = true
                        }
                        .tint(.red)
                    }
                }
                .padding(.horizontal)

                GroupBox(label: Text("Description")) {
                    VStack(alignment: .leading, spacing: 12) {
                        TextField("Description", text: $description)
                        TextField("Weight (optional, max 8 chars)", text: $weight)
                            .onChange(of: weight) { _, newValue in
                                if newValue.count > 8 {
                                    weight = String(newValue.prefix(8))
                                }
                            }
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Size (optional)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            SizeRadioGroup(selected: $size)
                        }
                    }
                }

                GroupBox(label: Text("Freezer")) {
                    VStack(alignment: .leading, spacing: 12) {
                        FreezerRadioGroup(freezers: freezers, selected: $freezerId)
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Shelf - counting from top: \(Int(shelfNumber))")
                            LineSlider(value: $shelfNumber, range: 1...Double(maxShelf), step: 1, tickCount: maxShelf)
                            HStack {
                                ForEach(1...maxShelf, id: \.self) { value in
                                    Text("\(value)")
                                        .font(.caption2)
                                        .frame(maxWidth: .infinity)
                                }
                            }
                        }
                    }
                }

                GroupBox(label: Text("Dates")) {
                    VStack(alignment: .leading, spacing: 12) {
                        DatePickerField(title: "Freeze date", date: $freezeDate)
                        DatePickerField(title: "Best before", date: $bestBefore)
                        Text("Use in \(daysUntil(bestBefore)) days")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
            .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .top) {
            if showSavedToast {
                Text("Item saved")
                    .font(.headline.weight(.semibold))
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(.regularMaterial, in: Capsule())
                    .overlay(
                        Capsule()
                            .stroke(Color.white.opacity(0.5), lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.22), radius: 14, x: 0, y: 8)
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .onChange(of: description) { _, _ in
            descriptionRevision += 1
            scheduleSave(revision: descriptionRevision)
        }
        .onChange(of: freezerId) { _, _ in scheduleSave(revision: descriptionRevision) }
        .onChange(of: freezeDate) { _, _ in scheduleSave(revision: descriptionRevision) }
        .onChange(of: bestBefore) { _, _ in scheduleSave(revision: descriptionRevision) }
        .onChange(of: shelfNumber) { _, _ in scheduleSave(revision: descriptionRevision) }
        .onChange(of: weight) { _, _ in scheduleSave(revision: descriptionRevision) }
        .onChange(of: size) { _, _ in scheduleSave(revision: descriptionRevision) }
        .alert("Delete item?", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) {
                onDelete(original)
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone.")
        }
    }

    private func scheduleSave(revision: Int? = nil) {
        if description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return
        }
        guard isDirty else { return }
        saveTask?.cancel()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            if let revision, revision != descriptionRevision {
                return
            }
            guard !Task.isCancelled, isDirty else { return }
            let freezerName = freezers.first(where: { $0.id == freezerId })?.name ?? item.freezerName
            let updated = Item(
                id: item.id,
                reference: item.reference,
                freezeDate: freezeDate.toApiString(),
                bestBefore: bestBefore.toApiString(),
                description: description,
                freezerId: freezerId,
                freezerName: freezerName,
                shelfNumber: Int(shelfNumber),
                weight: sanitisedWeight(weight),
                size: sanitisedSize(size)
            )
            await MainActor.run {
                original = updated
                onSave(updated)
                showSavedToast = true
            }
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            await MainActor.run {
                withAnimation(.easeOut(duration: 0.2)) {
                    showSavedToast = false
                }
            }
        }
    }

    private func daysUntil(_ date: Date) -> Int {
        let today = Calendar.current.startOfDay(for: Date())
        let target = Calendar.current.startOfDay(for: date)
        let components = Calendar.current.dateComponents([.day], from: today, to: target)
        return max(0, components.day ?? 0)
    }

    private func sanitisedWeight(_ rawValue: String) -> String? {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : String(trimmed.prefix(8))
    }

    private func sanitisedSize(_ rawValue: String) -> String? {
        switch rawValue {
        case "S", "M", "L":
            return rawValue
        default:
            return nil
        }
    }
}

private extension String {
    func toDate() -> Date {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: self) ?? Date()
    }
}

private extension Date {
    func toApiString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: self)
    }
}
