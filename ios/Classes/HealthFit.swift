//
//  HealthFit.swift
//  health_fit
//
//  Created by 小島徹也 on 2018/09/05.
//

import HealthKit

enum DataType {
    case step
    case weight

    init?(type: String) {
        switch type {
        case "DataType.STEP":
            self = .step
        case "DataType.WEIGHT":
            self = .weight
        default:
            return nil
        }
    }

    fileprivate var hkType: HKObjectType {
        switch self {
        case .step: return HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!
        case .weight: return HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.bodyMass)!
        }
    }
}

private extension HKAuthorizationStatus {
    var hasPhasPermission: Bool {
        switch self {
        case .notDetermined, .sharingDenied: return false
        case .sharingAuthorized: return true
        }
    }
}

final public class HealthFit {

    let health = HKHealthStore()

    func hasPermission(type: DataType) -> Bool {
        return health.authorizationStatus(for: type.hkType).hasPhasPermission
    }
}
