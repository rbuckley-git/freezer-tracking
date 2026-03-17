import Foundation

let apiKeyExpiryBuffer: TimeInterval = 24 * 60 * 60

func parseApiKeyExpiry(_ value: String) -> Date? {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = formatter.date(from: value) {
        return date
    }
    let fallback = ISO8601DateFormatter()
    fallback.formatOptions = [.withInternetDateTime]
    return fallback.date(from: value)
}

func isApiKeyValid(apiKey: String?, apiKeyExpiry: Date?) -> Bool {
    guard let apiKey, !apiKey.isEmpty, let expiry = apiKeyExpiry else {
        return false
    }
    return expiry.timeIntervalSinceNow > 0
}

func isApiKeyExpiringSoon(_ apiKeyExpiry: Date?) -> Bool {
    guard let expiry = apiKeyExpiry else {
        return true
    }
    return expiry.timeIntervalSinceNow <= apiKeyExpiryBuffer
}

func isCancellationError(_ error: Error) -> Bool {
    if error is CancellationError {
        return true
    }
    return (error as NSError).code == URLError.cancelled.rawValue
}
