import Shared

class KoinHelper {
    static let shared = KoinHelper()
    private var koin: Koin_coreKoin?

    private init() {}

    func start() {
        let platformModule = Koin_coreModule()
        let app = KoinInitKt.doInitKoin(platformModule: platformModule)
        koin = app.koin
    }

    func resolve<T: AnyObject>(_ type: T.Type) -> T {
        guard let koin = koin else { fatalError("Koin not started") }
        guard let result = koin.get(objCClass: type) as? T else {
            fatalError("Could not resolve \(type)")
        }
        return result
    }
}
