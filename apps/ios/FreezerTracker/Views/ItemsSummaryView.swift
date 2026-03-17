import SwiftUI

struct ItemsSummaryView: View {
    let freezers: [Freezer]
    let items: [Item]
    let onSave: (Item) -> Void
    let onDelete: (Item) -> Void

    @State private var selectedFreezerName: String = ""
    @State private var searchText: String = ""

    private var filteredItems: [Item] {
        let freezerFiltered: [Item]
        if selectedFreezerName.isEmpty {
            freezerFiltered = items
        } else {
            freezerFiltered = items.filter { $0.freezerName == selectedFreezerName }
        }
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else {
            return freezerFiltered
        }
        return freezerFiltered.filter { item in
            item.reference.localizedCaseInsensitiveContains(query) ||
            item.description.localizedCaseInsensitiveContains(query) ||
            item.freezerName.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 12) {
                VStack(spacing: 16) {
                    Text("Items")
                        .font(.headline)
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.horizontal)

                    if freezers.isEmpty {
                        Text("No freezers available.")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("Freezer", selection: $selectedFreezerName) {
                            Text("All").tag("")
                            ForEach(freezers) { freezer in
                                Text(freezer.name).tag(freezer.name)
                            }
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal)
                    }
                }
                .padding(.top, 12)

                ScrollView {
                    VStack(spacing: 8) {
                        if freezers.isEmpty {
                            EmptyView()
                        } else if filteredItems.isEmpty {
                            Text(searchText.isEmpty ? "No items available." : "No matching items.")
                                .foregroundColor(.secondary)
                        } else {
                            ForEach(filteredItems) { item in
                                NavigationLink(destination: ItemDetailView(item: item, freezers: freezers, onSave: onSave, onDelete: onDelete)) {
                                    HStack(alignment: .center, spacing: 12) {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text("\(item.reference) - \(item.description)")
                                                .font(.headline)
                                                .lineLimit(1)
                                            Text(detailLine(for: item))
                                                .font(.subheadline)
                                                .lineLimit(2)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .foregroundColor(.primary)
                                }
                                Divider()
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 12)
                }
                .frame(maxHeight: .infinity)

                if !freezers.isEmpty {
                    LiquidGlassSearchBar(text: $searchText, placeholder: "Search items")
                        .padding(.horizontal, 24)
                        .padding(.bottom, 8)
                }
            }
            .padding()
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
            .padding(.horizontal)
            .padding(.top)
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .top)
        }
        .onAppear {
            selectedFreezerName = ""
            searchText = ""
        }
    }

    private func daysUntil(_ dateString: String) -> Int {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: dateString) else {
            return 0
        }
        let today = Calendar.current.startOfDay(for: Date())
        let target = Calendar.current.startOfDay(for: date)
        let components = Calendar.current.dateComponents([.day], from: today, to: target)
        return max(0, components.day ?? 0)
    }

    private func detailLine(for item: Item) -> String {
        let days = daysUntil(item.bestBefore)
        if selectedFreezerName.isEmpty {
            return "\(item.freezerName) · Shelf \(item.shelfNumber) - use in \(days) days"
        }
        return "Shelf \(item.shelfNumber) - use in \(days) days"
    }
}

private struct LiquidGlassSearchBar: View {
    @Binding var text: String
    let placeholder: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.35), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.18), radius: 12, x: 0, y: 6)
    }
}
