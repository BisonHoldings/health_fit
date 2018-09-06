//
//  HealthFit.swift
//  health_fit
//
//  Created by 小島徹也 on 2018/09/05.
//

import HealthKit

enum DataType: String {
    case step = "DataType.STEP"
    case weight = "DataType.WEIGHT"

    fileprivate var hkType: HKQuantityType {
        switch self {
        case .step: return HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!
        case .weight: return HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.bodyMass)!
        }
    }

    fileprivate var unit: HKUnit {
        switch self {
        case .step: return HKUnit.count()
        case .weight: return HKUnit.gram()
        }
    }
}

enum Permission: Int {
    case read
    case write
    case readAndWrite
}

enum TimeUnit: String {
    case milliseconds = "TimeUnit.MILLISECONDS"
    case hours = "TimeUnit.HOURS"

    var dateComponent: DateComponents {
        switch self {
        case .hours:
            return DateComponents(hour: 1)
        case .milliseconds:
            return DateComponents(nanosecond: 1_000_000)
        }
    }
}

private extension HKAuthorizationStatus {
    var hasPermission: Bool {
        switch self {
        case .notDetermined, .sharingDenied: return false
        case .sharingAuthorized: return true
        }
    }
}

final class HealthFit {

    private let health = HKHealthStore()

    func hasPermission(type: DataType) -> Bool {
        return health.authorizationStatus(for: type.hkType).hasPermission
    }

    func requestPermission(type: DataType, permission: Permission, completion: @escaping (Bool, Error?) -> Void) {
        let shareType: Set<HKSampleType>?
        let readType: Set<HKObjectType>?
        switch permission {
        case .read:
            shareType = nil
            readType = [type.hkType]
        case .write:
            shareType = [type.hkType]
            readType = nil
        case .readAndWrite:
            shareType = [type.hkType]
            readType = [type.hkType]
        }
        health.requestAuthorization(toShare: shareType, read: readType, completion: completion)
    }

    func getData(startDate: Date, endDate: Date, dataType: DataType, timeUnite: TimeUnit, completion: @escaping ((String) -> Void)) {

        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictEndDate)
        let anchorDate = Calendar(identifier: Calendar.Identifier.gregorian).startOfDay(for: Date())
        
        let query = HKStatisticsCollectionQuery(
            quantityType: dataType.hkType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum,
            anchorDate: anchorDate,
            intervalComponents: timeUnite.dateComponent
        )
        let handler: ((HKStatisticsCollectionQuery, HKStatisticsCollection?, Error?) -> Void)? = { query, collection, error in
            debugPrint(query, collection, error)
            let quantities = collection?.statistics().compactMap { $0.sumQuantity() } ?? []
            let values = quantities.map { $0.doubleValue(for: dataType.unit)}

            completion(values.description)
        }
        query.initialResultsHandler = handler

        health.execute(query)
    }
}
