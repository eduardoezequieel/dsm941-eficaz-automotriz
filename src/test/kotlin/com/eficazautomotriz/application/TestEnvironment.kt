package com.eficazautomotriz.application

import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.AppointmentStatus
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.ServiceStatus
import com.eficazautomotriz.domain.model.SystemConfig
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.repository.AppointmentRepository
import com.eficazautomotriz.domain.repository.MaintenanceRecordRepository
import com.eficazautomotriz.domain.repository.MaintenanceTypeRepository
import com.eficazautomotriz.domain.repository.ServiceOrderRepository
import com.eficazautomotriz.domain.repository.VehicleRepository
import java.time.LocalDate

class TestEnvironment(
    val vehicles: MutableList<Vehicle> = mutableListOf(),
    val maintenanceTypes: MutableList<MaintenanceType> = mutableListOf(),
    val maintenanceMileage: MutableMap<Pair<String, String>, Int> = mutableMapOf(),
    val appointmentsInPeriod: MutableList<Appointment> = mutableListOf(),
    val allAppointments: MutableList<Appointment> = appointmentsInPeriod,
    val ordersInPeriod: MutableList<ServiceOrder> = mutableListOf(),
    val allOrders: MutableList<ServiceOrder> = ordersInPeriod,
    val systemConfig: SystemConfig = systemConfig()
) {
    val vehicleRepository: VehicleRepository = FakeVehicleRepository(vehicles)
    val maintenanceRecordRepository: MaintenanceRecordRepository =
        FakeMaintenanceRecordRepository(maintenanceMileage)
    val maintenanceTypeRepository: MaintenanceTypeRepository =
        FakeMaintenanceTypeRepository(maintenanceTypes)
    val appointmentRepository: AppointmentRepository =
        FakeAppointmentRepository(appointmentsInPeriod, allAppointments)
    val serviceOrderRepository: ServiceOrderRepository =
        FakeServiceOrderRepository(ordersInPeriod, allOrders)

    val maintenanceService = MaintenanceService(
        vehicleRepository = vehicleRepository,
        maintenanceRecordRepository = maintenanceRecordRepository,
        maintenanceTypeRepository = maintenanceTypeRepository,
        systemConfig = systemConfig
    )

    val reportService = ReportService(
        vehicleRepository = vehicleRepository,
        appointmentRepository = appointmentRepository,
        serviceOrderRepository = serviceOrderRepository,
        maintenanceService = maintenanceService
    )

    companion object {
        fun vehicle(
            id: String,
            currentMileage: Int
        ): Vehicle {
            return Vehicle(
                id = id,
                currentMileage = currentMileage
            )
        }

        fun maintenanceType(
            id: String,
            name: String = id,
            intervalMileage: Int = 5_000,
            active: Boolean = true
        ): MaintenanceType {
            return MaintenanceType(
                id = id,
                name = name,
                intervalMileage = intervalMileage,
                active = active
            )
        }

        fun appointment(
            status: AppointmentStatus
        ): Appointment {
            return Appointment(
                status = status
            )
        }

        fun serviceOrder(
            status: ServiceStatus,
            serviceType: String
        ): ServiceOrder {
            return ServiceOrder(
                status = status,
                serviceType = serviceType
            )
        }

        fun systemConfig(
            maintenanceWarningThreshold: Int = 500
        ): SystemConfig {
            return SystemConfig(
                defaultSlotCapacity = 2,
                maintenanceWarningThreshold = maintenanceWarningThreshold
            )
        }
    }
}

private class FakeVehicleRepository(
    private val vehicles: MutableList<Vehicle>
) : VehicleRepository {
    override fun findById(id: String): Vehicle? {
        return vehicles.find { vehicle -> vehicle.id == id }
    }

    override fun findAll(): List<Vehicle> {
        return vehicles.toList()
    }
}

private class FakeMaintenanceTypeRepository(
    private val maintenanceTypes: MutableList<MaintenanceType>
) : MaintenanceTypeRepository {
    override fun findActive(): List<MaintenanceType> {
        return maintenanceTypes.filter { maintenanceType -> maintenanceType.active }
    }

    override fun findById(id: String): MaintenanceType? {
        return maintenanceTypes.find { maintenanceType -> maintenanceType.id == id }
    }
}

private class FakeMaintenanceRecordRepository(
    private val maintenanceMileage: MutableMap<Pair<String, String>, Int>
) : MaintenanceRecordRepository {
    override fun findLastMileage(
        vehicleId: String,
        maintenanceTypeId: String
    ): Int? {
        return maintenanceMileage[vehicleId to maintenanceTypeId]
    }

    override fun saveBaseMileage(
        vehicleId: String,
        maintenanceTypeId: String,
        mileage: Int
    ) {
        maintenanceMileage[vehicleId to maintenanceTypeId] = mileage
    }
}

private class FakeAppointmentRepository(
    private val appointmentsInPeriod: MutableList<Appointment>,
    private val allAppointments: MutableList<Appointment>
) : AppointmentRepository {
    override fun findByPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Appointment> {
        return appointmentsInPeriod.toList()
    }

    override fun findAll(): List<Appointment> {
        return allAppointments.toList()
    }
}

private class FakeServiceOrderRepository(
    private val ordersInPeriod: MutableList<ServiceOrder>,
    private val allOrders: MutableList<ServiceOrder>
) : ServiceOrderRepository {
    override fun findByPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ServiceOrder> {
        return ordersInPeriod.toList()
    }

    override fun findAll(): List<ServiceOrder> {
        return allOrders.toList()
    }
}
