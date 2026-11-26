import Foundation

struct LyricsLine: Hashable {
    let time: Double
    let text: String
}

enum LyricsResult {
    case synced([LyricsLine])
    case plain(String)
    case none
}

/// Minimal ID3v2.2/2.3/2.4 USLT and SYLT reader. AVFoundation does not expose
/// SYLT, so Lowspin reads the frames straight from the imported file.
enum ID3Lyrics {
    static func read(from track: Track) -> LyricsResult {
        var stale = false
        guard let folder = try? URL(
            resolvingBookmarkData: track.folderBookmark,
            options: [],
            relativeTo: nil,
            bookmarkDataIsStale: &stale
        ), folder.startAccessingSecurityScopedResource() else { return .none }
        defer { folder.stopAccessingSecurityScopedResource() }

        guard let data = try? Data(contentsOf: folder.appendingPathComponent(track.relativePath)) else {
            return .none
        }
        return parse(data)
    }

    static func parse(_ data: Data) -> LyricsResult {
        let all = frames(in: data)
        for frame in all where frame.id == "SYLT" {
            if let lines = parseSYLT(frame.body) { return .synced(lines) }
        }
        for frame in all where frame.id == "USLT" {
            if let text = parseUSLT(frame.body), !text.isEmpty { return .plain(text) }
        }
        return .none
    }

    // MARK: - Frames

    private static func frames(in data: Data) -> [(id: String, body: Data)] {
        guard data.count >= 10,
              String(data: data.subdata(in: 0..<3), encoding: .ascii) == "ID3"
        else { return [] }

        let version = data[3]
        let tagFlags = data[5]
        var cursor = 10

        if tagFlags & 0x40 != 0 {
            let size = version >= 4 ? synchsafe(data, cursor) : be32(data, cursor)
            cursor += max(0, Int(size)) + (version >= 4 ? 0 : 4)
        }

        var result: [(String, Data)] = []
        while cursor + 10 <= data.count {
            guard let id = String(data: data.subdata(in: cursor..<(cursor + 4)), encoding: .ascii),
                  !id.isEmpty, id.first != "\0"
            else { break }

            let size = version >= 4 ? synchsafe(data, cursor + 4) : be32(data, cursor + 4)
            guard size > 0, cursor + 10 + size <= data.count else { break }

            var body = data.subdata(in: (cursor + 10)..<(cursor + 10 + size))
            let frameFlags = data[cursor + 9]
            let unsynced = (tagFlags & 0x80 != 0)
                || (version >= 4 ? frameFlags & 0x02 != 0 : frameFlags & 0x80 != 0)
            if unsynced { body = deunsynchronize(body) }

            result.append((id, body))
            cursor += 10 + size
        }
        return result
    }

    private static func be32(_ data: Data, _ offset: Int) -> Int {
        guard offset + 4 <= data.count else { return 0 }
        return (Int(data[offset]) << 24)
            | (Int(data[offset + 1]) << 16)
            | (Int(data[offset + 2]) << 8)
            | Int(data[offset + 3])
    }

    private static func synchsafe(_ data: Data, _ offset: Int) -> Int {
        guard offset + 4 <= data.count else { return 0 }
        return (Int(data[offset] & 0x7F) << 21)
            | (Int(data[offset + 1] & 0x7F) << 14)
            | (Int(data[offset + 2] & 0x7F) << 7)
            | Int(data[offset + 3] & 0x7F)
    }

    private static func deunsynchronize(_ data: Data) -> Data {
        let bytes = [UInt8](data)
        var output = Data()
        var index = 0
        while index < bytes.count {
            output.append(bytes[index])
            if bytes[index] == 0xFF, index + 1 < bytes.count, bytes[index + 1] == 0x00 {
                index += 2
            } else {
                index += 1
            }
        }
        return output
    }

    // MARK: - Text frames

    private static func terminator(for encoding: UInt8) -> [UInt8] {
        encoding == 1 || encoding == 2 ? [0, 0] : [0]
    }

    private static func decode(_ data: Data, _ encoding: UInt8) -> String? {
        switch encoding {
        case 0: return String(data: data, encoding: .isoLatin1)
        case 1: return String(data: data, encoding: .utf16)
        case 2: return String(data: data, encoding: .utf16BigEndian)
        case 3: return String(data: data, encoding: .utf8)
        default: return String(data: data, encoding: .isoLatin1)
        }
    }

    private static func skipTerminator(_ bytes: [UInt8], _ encoding: UInt8, from start: Int) -> Int {
        let mark = terminator(for: encoding)
        var index = start
        while index + mark.count <= bytes.count {
            var matches = true
            for offset in 0..<mark.count where bytes[index + offset] != mark[offset] {
                matches = false
                break
            }
            if matches { return index + mark.count }
            index += 1
        }
        return start
    }

    private static func parseUSLT(_ body: Data) -> String? {
        let bytes = [UInt8](body)
        guard bytes.count > 4 else { return nil }
        let encoding = bytes[0]
        let start = skipTerminator(bytes, encoding, from: 4)
        guard start < bytes.count else { return nil }
        return decode(Data(bytes[start...]), encoding)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func parseSYLT(_ body: Data) -> [LyricsLine]? {
        let bytes = [UInt8](body)
        guard bytes.count > 7 else { return nil }

        let encoding = bytes[0]
        let timestampFormat = bytes[4]
        let mark = terminator(for: encoding)
        var cursor = skipTerminator(bytes, encoding, from: 6)
        var lines: [LyricsLine] = []

        while cursor < bytes.count {
            var end = cursor
            while end + mark.count <= bytes.count {
                var matches = true
                for offset in 0..<mark.count where bytes[end + offset] != mark[offset] {
                    matches = false
                    break
                }
                if matches { break }
                end += 1
            }
            let stamp = end + mark.count
            guard stamp + 4 <= bytes.count else { break }

            let raw = (UInt32(bytes[stamp]) << 24)
                | (UInt32(bytes[stamp + 1]) << 16)
                | (UInt32(bytes[stamp + 2]) << 8)
                | UInt32(bytes[stamp + 3])
            let seconds = timestampFormat == 1 ? Double(raw) / 30.0 : Double(raw) / 1000.0

            if let text = decode(Data(bytes[cursor..<end]), encoding), !text.isEmpty {
                lines.append(LyricsLine(time: seconds, text: text))
            }
            cursor = stamp + 4
        }

        return lines.isEmpty ? nil : lines.sorted { $0.time < $1.time }
    }
}

enum LyricsLoader {
    private static var cache: [String: LyricsResult] = [:]

    static func lyrics(for track: Track) -> LyricsResult {
        if let hit = cache[track.id] { return hit }
        let result = ID3Lyrics.read(from: track)
        cache[track.id] = result
        return result
    }
}
