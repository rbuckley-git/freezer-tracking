import Foundation

struct APIClient {
    var baseURL: String
    var apiKey: String?
    var apiKeyExpiry: Date?

    func listFreezers() async throws -> [Freezer] {
        return try await request(path: "/freezers")
    }

    func listItems() async throws -> ItemList {
        return try await listItems(page: 0, size: 100)
    }

    func listItems(page: Int, size: Int) async throws -> ItemList {
        return try await request(path: "/items?page=\(page)&size=\(size)")
    }

    func listAllItems(pageSize: Int = 100) async throws -> [Item] {
        var page = 0
        var items: [Item] = []

        while true {
            let response = try await listItems(page: page, size: pageSize)
            items.append(contentsOf: response.items)
            if page >= response.totalPages - 1 {
                break
            }
            page += 1
        }

        return items
    }

    func createFreezer(_ payload: FreezerPayload) async throws -> Freezer {
        return try await request(path: "/freezers", method: "POST", body: payload)
    }

    func updateFreezer(id: Int, payload: FreezerPayload) async throws -> Freezer {
        return try await request(path: "/freezers/\(id)", method: "PUT", body: payload)
    }

    func deleteFreezer(id: Int) async throws {
        let _: EmptyResponse = try await request(path: "/freezers/\(id)", method: "DELETE")
    }

    func createItem(_ payload: ItemCreatePayload) async throws -> Item {
        return try await request(path: "/items", method: "POST", body: payload)
    }

    func updateItem(id: UUID, payload: ItemCreatePayload) async throws -> Item {
        return try await request(path: "/items/\(id.uuidString)", method: "PUT", body: payload)
    }

    func deleteItem(id: UUID) async throws {
        let _: EmptyResponse = try await request(path: "/items/\(id.uuidString)", method: "DELETE")
    }

    func login(username: String, password: String) async throws -> LoginResponse {
        struct LoginPayload: Encodable {
            let username: String
            let password: String
            let clientType: String
        }
        return try await request(
            path: "/login",
            method: "POST",
            body: LoginPayload(username: username, password: password, clientType: "IOS")
        )
    }

    func nextReference() async throws -> NextReferenceResponse {
        return try await request(path: "/items/next-reference")
    }

    private func request<T: Decodable>(path: String, method: String = "GET", body: Encodable? = nil) async throws -> T {
        guard let url = URL(string: baseURL + path) else {
            throw URLError(.badURL)
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        if let apiKey, isApiKeyValid(apiKey: apiKey, apiKeyExpiry: apiKeyExpiry) {
            request.addValue(apiKey, forHTTPHeaderField: "X-API-Key")
        }
        if let body {
            request.httpBody = try JSONEncoder().encode(AnyEncodable(body))
        }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        if !(200..<300).contains(httpResponse.statusCode) {
            if let apiError = try? JSONDecoder().decode(ApiError.self, from: data) {
                throw APIClientError.server(apiError.message)
            }
            throw APIClientError.server("Request failed: \(httpResponse.statusCode)")
        }
        if httpResponse.statusCode == 204 {
            return EmptyResponse() as! T
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}

struct ItemCreatePayload: Encodable {
    let reference: String
    let freezeDate: String
    let bestBefore: String
    let description: String
    let freezerId: Int
    let shelfNumber: Int
    let weight: String?
    let size: String?
}

struct FreezerPayload: Encodable {
    let name: String
    let shelfCount: Int
}

struct ApiError: Decodable {
    let message: String
}

struct NextReferenceResponse: Decodable {
    let nextReference: String
}

struct EmptyResponse: Decodable {}

struct LoginResponse: Decodable {
    let apiKey: String
    let apiKeyExpiry: String
    let isAdmin: Bool
}

enum APIClientError: Error, LocalizedError {
    case server(String)

    var errorDescription: String? {
        switch self {
        case .server(let message):
            return message
        }
    }
}

struct AnyEncodable: Encodable {
    private let encodeFunc: (Encoder) throws -> Void

    init(_ encodable: Encodable) {
        self.encodeFunc = encodable.encode
    }

    func encode(to encoder: Encoder) throws {
        try encodeFunc(encoder)
    }
}
