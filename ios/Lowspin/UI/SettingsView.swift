import SwiftUI

struct SettingsView: View {
    @ObservedObject private var appearance = Appearance.shared
    @ObservedObject private var equalizer = EqualizerSettings.shared
    @ObservedObject private var sleepTimer = SleepTimer.shared
    @State private var showSleepTimer = false

    var body: some View {
        NavigationStack {
            List {
                Section("Appearance") {
                    ForEach(Appearance.options) { option in
                        Button {
                            withAnimation(appearance.reduceMotion ? nil : .easeOut(duration: 0.22)) {
                                appearance.accentIndex = option.id
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Circle()
                                    .fill(Color(uiColor: option.color))
                                    .frame(width: 22, height: 22)
                                Text(option.name)
                                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                                Spacer()
                                if appearance.accentIndex == option.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(Color(uiColor: appearance.palette.accent))
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }

                Section("Accessibility") {
                    Toggle("High contrast text", isOn: Binding(
                        get: { appearance.highContrast },
                        set: { appearance.highContrast = $0 }
                    ))
                    Toggle("Reduce motion", isOn: Binding(
                        get: { appearance.reduceMotion },
                        set: { appearance.reduceMotion = $0 }
                    ))
                }

                Section("Sound") {
                    Button {
                        NotificationCenter.default.post(name: .openEqualizer, object: nil)
                    } label: {
                        HStack {
                            Text("Equaliser").foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                            Spacer()
                            Text(equalizer.enabled ? EqualizerSettings.name(equalizer.presetId) : "Off")
                                .font(.caption)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        }
                    }
                    .buttonStyle(.plain)

                    Button {
                        showSleepTimer = true
                    } label: {
                        HStack {
                            Text("Sleep Timer").foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                            Spacer()
                            Text(sleepTimer.isActive ? "Running" : "Off")
                                .font(.caption)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        }
                    }
                    .buttonStyle(.plain)
                }

                Section("Library") {
                    NavigationLink("Folders") { FoldersView() }
                    NavigationLink("Hidden Songs") { HiddenSongsView() }
                }

                Section {
                    NavigationLink("About") { AboutView() }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color(uiColor: appearance.palette.background).ignoresSafeArea())
            .navigationTitle("Settings")
            .sheet(isPresented: $showSleepTimer) { SleepTimerSheet() }
        }
    }
}
