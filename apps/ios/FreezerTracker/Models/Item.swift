import Foundation

struct Item: Codable, Identifiable {
    let id: UUID
    let reference: String
    let freezeDate: String
    let bestBefore: String
    let description: String
    let freezerId: Int
    let freezerName: String
    let shelfNumber: Int
    let weight: String?
    let size: String?
}

struct ItemList: Codable {
    let items: [Item]
    let page: Int
    let size: Int
    let totalItems: Int
    let totalPages: Int
}
