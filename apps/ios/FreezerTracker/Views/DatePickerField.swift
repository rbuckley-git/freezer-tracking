import SwiftUI

struct DatePickerField: View {
    let title: String
    @Binding var date: Date
    @State private var isPresented = false

    var body: some View {
        Button {
            isPresented = true
        } label: {
            HStack {
                Text(title)
                Spacer()
                Text(date.formatted(date: .numeric, time: .omitted))
                    .foregroundColor(.secondary)
            }
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $isPresented) {
            VStack(spacing: 16) {
                DatePicker(title, selection: $date, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .onChange(of: date) {
                        isPresented = false
                    }
                Button("Done") {
                    isPresented = false
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
    }
}
