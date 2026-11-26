import SwiftUI

struct SleepTimerSheet: View {
    @ObservedObject private var timer = SleepTimer.shared
    @ObservedObject private var appearance = Appearance.shared
    @Environment(\.dismiss) private var dismiss

    private let options: [(label: String, minutes: Double)] = [
        ("5 minutes", 5), ("10 minutes", 10), ("15 minutes", 15),
        ("30 minutes", 30), ("45 minutes", 45), ("60 minutes", 60)
    ]

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()
            VStack(spacing: 18) {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            .frame(width: 40, height: 40)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Text("Sleep Timer")
                        .font(.headline)
                        .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    Spacer()
                    Color.clear.frame(width: 40)
                }

                Image(systemName: "moon.zzz")
                    .font(.system(size: 48))
                    .foregroundColor(Color(uiColor: appearance.palette.accent))

                if timer.isActive {
                    Text("Playback pauses when the timer ends")
                        .font(.subheadline)
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    Button("Cancel Timer", role: .destructive) { timer.cancel() }
                } else {
                    ForEach(options, id: \.label) { option in
                        Button {
                            timer.start(minutes: option.minutes, label: option.label)
                            dismiss()
                        } label: {
                            HStack {
                                Text(option.label)
                                Spacer()
                                Image(systemName: "chevron.right")
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(Color(uiColor: appearance.palette.surface))
                            .cornerRadius(10)
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Spacer()
            }
            .padding()
        }
    }
}
