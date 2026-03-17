import SwiftUI

struct LineSlider: View {
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let tickCount: Int

    private let trackHeight: CGFloat = 2
    private let thumbSize: CGFloat = 18

    var body: some View {
        GeometryReader { proxy in
            let width = max(1, proxy.size.width)
            let tickInset = tickCount > 1 ? width / CGFloat(tickCount * 2) : 0
            let usableWidth = max(1, width - (tickInset * 2))
            let clampedValue = min(max(value, range.lowerBound), range.upperBound)
            let ratio = (range.upperBound - range.lowerBound) == 0
                ? 0
                : (clampedValue - range.lowerBound) / (range.upperBound - range.lowerBound)
            let x = tickInset + (CGFloat(ratio) * usableWidth)

            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.secondary.opacity(0.35))
                    .frame(height: trackHeight)
                    .padding(.horizontal, tickInset)
                Circle()
                    .fill(Color.accentColor)
                    .frame(width: thumbSize, height: thumbSize)
                    .offset(x: x - thumbSize / 2)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { gesture in
                        let location = min(max(gesture.location.x - tickInset, 0), usableWidth)
                        let ratio = location / usableWidth
                        let raw = range.lowerBound + Double(ratio) * (range.upperBound - range.lowerBound)
                        let stepped = (raw / step).rounded() * step
                        value = min(max(stepped, range.lowerBound), range.upperBound)
                    }
            )
        }
        .frame(height: thumbSize)
        .accessibilityValue(Text("\(Int(value))"))
    }
}
