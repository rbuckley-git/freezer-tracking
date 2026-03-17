import SwiftUI

struct SettingsView: View {
    @ObservedObject var appState: AppState

#if DEBUG
    @AppStorage("apiBaseUrl") private var apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#else
    private let apiBaseUrl: String = "https://freezer-api.learnsharegrow.io"
#endif
    @AppStorage("apiKey") private var apiKey: String = ""
    @AppStorage("apiKeyExpiry") private var apiKeyExpiry: String = ""
    @AppStorage("username") private var storedUsername: String = ""
    @AppStorage("isAdmin") private var isAdmin: Bool = false

    @State private var username = ""
    @State private var password = ""
    @State private var isLoggingIn = false
    @State private var loginMessage: String?

    private var expiryDate: Date? {
        parseApiKeyExpiry(apiKeyExpiry)
    }

    private var isSignedIn: Bool {
        isApiKeyValid(apiKey: apiKey, apiKeyExpiry: expiryDate)
    }

    private var appVersionText: String {
        let shortVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
        return shortVersion ?? "Unknown"
    }

    private var statusTitle: String {
        if isSignedIn {
            return "Signed in"
        }
        if !apiKey.isEmpty {
            return "Session expired"
        }
        return "Not signed in"
    }

    private var statusTint: Color {
        if isSignedIn {
            return .green
        }
        if !apiKey.isEmpty {
            return .orange
        }
        return .secondary
    }

    var body: some View {
        VStack {
            VStack(spacing: 16) {
                Text("Settings")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.horizontal)

                Form {
#if DEBUG
                    Section("Server address") {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("API base URL")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("API Base URL", text: $apiBaseUrl, axis: .vertical)
                                .textInputAutocapitalization(.never)
                                .keyboardType(.URL)
                                .autocorrectionDisabled()
                                .textFieldStyle(.roundedBorder)
                        }
                    }
#endif
                    Section("Session status") {
                        VStack(alignment: .leading, spacing: 12) {
                            Label(statusTitle, systemImage: isSignedIn ? "checkmark.circle.fill" : "info.circle.fill")
                                .font(.headline)
                                .foregroundStyle(statusTint)

                            if isSignedIn {
                                statusValue("Account", value: storedUsername.isEmpty ? "Signed in" : storedUsername)
                                statusValue(
                                    "Expires",
                                    value: expiryDate?.formatted(date: .abbreviated, time: .shortened) ?? "Unknown"
                                )
                            } else if !apiKey.isEmpty {
                                Text("Your saved session is no longer valid. Sign in again to continue.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            } else {
                                Text("No active session is stored on this device.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 6)

                        if isSignedIn {
                            Button("Sign out") {
                                apiKey = ""
                                apiKeyExpiry = ""
                                storedUsername = ""
                                isAdmin = false
                                appState.reset()
                                appState.freezers = []
                                appState.items = []
                                appState.item = nil
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }

                    if isSignedIn && isAdmin {
                        Section("Administration") {
                            NavigationLink("Setup freezers") {
                                FreezerSetupView(appState: appState)
                            }
                        }
                    }

                    if !isSignedIn {
                        Section("Sign in") {
                            LabeledContent("Email") {
                                TextField("Email", text: $username)
                                    .textInputAutocapitalization(.never)
                                    .keyboardType(.emailAddress)
                                    .autocorrectionDisabled()
                                    .multilineTextAlignment(.trailing)
                            }
                            LabeledContent("Password") {
                                SecureField("Password", text: $password)
                                    .multilineTextAlignment(.trailing)
                            }

                            Button(isLoggingIn ? "Signing in..." : "Sign in") {
                                Task { await login() }
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.blue)
                            .disabled(isLoggingIn || username.isEmpty || password.isEmpty)
                            .frame(maxWidth: .infinity, alignment: .center)

                            if let loginMessage {
                                Text(loginMessage)
                                    .font(.footnote)
                                    .foregroundStyle(loginMessage == "Signed in." ? .green : .secondary)
                            }
                        }
                    }

                    Section("Version") {
                        Text(appVersionText)
                            .foregroundStyle(.secondary)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .padding()
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
            .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .navigationTitle("")
        .onAppear {
            clearExpiredKeyIfNeeded()
        }
    }

    private func login() async {
        isLoggingIn = true
        loginMessage = nil
        defer { isLoggingIn = false }
        do {
            let client = APIClient(baseURL: apiBaseUrl, apiKey: nil, apiKeyExpiry: nil)
            let response = try await client.login(username: username, password: password)
            apiKey = response.apiKey
            apiKeyExpiry = response.apiKeyExpiry
            storedUsername = username
            isAdmin = response.isAdmin
            password = ""
            loginMessage = "Signed in."
        } catch {
            if isCancellationError(error) {
                loginMessage = nil
                return
            }
            loginMessage = error.localizedDescription
        }
    }

    private func clearExpiredKeyIfNeeded() {
        guard !apiKey.isEmpty else { return }
        guard let expiryDate else {
            apiKey = ""
            apiKeyExpiry = ""
            isAdmin = false
            return
        }
        if expiryDate <= Date() {
            apiKey = ""
            apiKeyExpiry = ""
            isAdmin = false
        }
    }

    @ViewBuilder
    private func statusValue(_ label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }
}
