import CoreImage
import Foundation
import UIKit

enum ArtworkProcessor {
    /// Small JPEG kept in SwiftData for lists, the mini player and Now Playing.
    static func thumbnail(from data: Data) -> Data? {
        guard let image = UIImage(data: data) else { return nil }
        return image.scaled(to: 256).jpegData(compressionQuality: 0.75)
    }

    /// Average colour of the cover, cached per track at scan time.
    static func dominantColor(from data: Data) -> Int? {
        guard let image = UIImage(data: data),
              let cgImage = image.scaled(to: 48).cgImage
        else { return nil }

        let source = CIImage(cgImage: cgImage)
        guard let filter = CIFilter(name: "CIAreaAverage", parameters: [
            kCIInputImageKey: source,
            kCIInputExtentKey: CIVector(cgRect: source.extent)
        ]), let output = filter.outputImage else { return nil }

        var rgba = [UInt8](repeating: 0, count: 4)
        CIContext().render(
            output,
            toBitmap: &rgba,
            rowBytes: 4,
            bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
            format: .RGBA8,
            colorSpace: nil
        )
        return (Int(rgba[3]) << 24)
            | (Int(rgba[0]) << 16)
            | (Int(rgba[1]) << 8)
            | Int(rgba[2])
    }
}
