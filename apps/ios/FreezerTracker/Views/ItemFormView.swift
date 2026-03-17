import SwiftUI

struct ItemFormView: View {
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
    let onSubmit: () -> Void

    private var maxShelf: Int {
        let shelfCount = freezers.first(where: { $0.id == freezerId })?.shelfCount ?? 10
        return max(1, shelfCount)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            GroupBox(label: Text("Item details")) {
                VStack(alignment: .leading, spacing: 12) {
                    TextField("Reference", text: $reference)
                        .keyboardType(.numberPad)
                        .disabled(true)
                        .textFieldStyle(.roundedBorder)
                    TextField("Description", text: $description)
                        .disabled(isDisabled)
                        .textFieldStyle(.roundedBorder)
                    TextField("Weight (optional, max 8 chars)", text: $weight)
                        .disabled(isDisabled)
                        .textFieldStyle(.roundedBorder)
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
                    .disabled(isDisabled)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Freezer")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        FreezerRadioGroup(freezers: freezers, selected: $freezerId)
                    }
                    .disabled(isDisabled)

                    DatePickerField(title: "Freeze date", date: $freezeDate)
                        .disabled(isDisabled)
                    DatePickerField(title: "Best before", date: $bestBefore)
                        .disabled(isDisabled)
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Shelf - counting from top: \(Int(shelfNumber))")
                        LineSlider(value: $shelfNumber, range: 1...Double(maxShelf), step: 1, tickCount: maxShelf)
                            .disabled(isDisabled)
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
        }
        .onAppear {
            if freezerId == 0, let first = freezers.first {
                freezerId = first.id
            }
        }
    }
}

struct SizeRadioGroup: View {
    @Binding var selected: String

    private let options: [(label: String, value: String)] = [
        ("Not set", ""),
        ("Small (S)", "S"),
        ("Medium (M)", "M"),
        ("Large (L)", "L")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(options, id: \.value) { option in
                Button {
                    selected = option.value
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: selected == option.value ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(.accentColor)
                        Text(option.label)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel(option.label)
                .accessibilityAddTraits(selected == option.value ? .isSelected : [])
            }
        }
    }
}
