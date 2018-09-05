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
}

enum Permission: Int {
    case read
    case write
    case readAndWrite
}

enum TimeUnit: String {
    case milliseconds = "TimeUnit.MILLISECONDS"
    case hours = "TimeUnit.HOURS"
}

private extension HKAuthorizationStatus {
    var hasPhasPermission: Bool {
        switch self {
        case .notDetermined, .sharingDenied: return false
        case .sharingAuthorized: return true
        }
    }
}

final class HealthFit {

    private let health = HKHealthStore()

    func hasPermission(type: DataType) -> Bool {
        return health.authorizationStatus(for: type.hkType).hasPhasPermission
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
}
