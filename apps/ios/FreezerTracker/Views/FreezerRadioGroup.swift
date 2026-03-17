import SwiftUI

struct FreezerRadioGroup: View {
    let freezers: [Freezer]
    @Binding var selected: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(freezers) { freezer in
                Button(action: { selected = freezer.id }) {
                    HStack {
                        Image(systemName: selected == freezer.id ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(.accentColor)
                        Text(freezer.name)
                            .foregroundColor(.primary)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }
}
