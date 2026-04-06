package com.project666.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.LabTestStatusEnum;
import com.project666.backend.repository.AppointmentBillRepository;
import com.project666.backend.repository.LabBillRepository;
import com.project666.backend.service.BillService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService{

    private final BigDecimal CANCELLATION_FEE_PERCENTAGE = new BigDecimal("0.05");
    private final BigDecimal LATE_FEE_AMOUNT = new BigDecimal("10.00");
    private final BigDecimal LAB_TEST_FEE = new BigDecimal("15.00");

    private final Map<BillTypeEnum, BigDecimal> baseFeeMap = Map.of(
        BillTypeEnum.QUICK_CHECK_FEE, new BigDecimal("15.00"),
        BillTypeEnum.MID_CHECK_FEE, new BigDecimal("20.00"),
        BillTypeEnum.LONG_CHECK_FEE, new BigDecimal("25.00")
    );
    private final Map<AppointmentTypeEnum, BillTypeEnum> appointmentBillMap = Map.of(
        AppointmentTypeEnum.QUICK_CHECK, BillTypeEnum.QUICK_CHECK_FEE,
        AppointmentTypeEnum.MID_CHECK, BillTypeEnum.MID_CHECK_FEE,
        AppointmentTypeEnum.LONG_CHECK, BillTypeEnum.LONG_CHECK_FEE
    );
    private final Map<AppointmentTypeEnum, BillTypeEnum> appointmentCancelBillMap = Map.of(
        AppointmentTypeEnum.QUICK_CHECK, BillTypeEnum.QUICK_CHECK_CANCELLATION_FEE,
        AppointmentTypeEnum.MID_CHECK, BillTypeEnum.MID_CHECK_CANCELLATION_FEE,
        AppointmentTypeEnum.LONG_CHECK, BillTypeEnum.LONG_CHECK_CANCELLATION_FEE
    );

    private final AppointmentBillRepository appointmentBillRepository;
    private final LabBillRepository labBillRepository;

    @Override
    @Transactional
    public AppointmentBill generateBillForAppointment(Appointment appointment) {

        AppointmentTypeEnum appointmentType = appointment.getType();
        BillTypeEnum billType = appointmentBillMap.get(appointmentType);
        if (billType == null) throw new IllegalArgumentException();
        BigDecimal amount = baseFeeMap.get(billType);

        return appointmentBillRepository.save(
            createAppointmentBill(appointment, billType, amount)
        );
    }

    @Override
    @Transactional
    public AppointmentBill generateLateFeeBill(Appointment appointment) {
        return appointmentBillRepository.save(
            createAppointmentBill(appointment, BillTypeEnum.LATE_FEE, LATE_FEE_AMOUNT)
        );
    }

    @Override
    @Transactional
    public AppointmentBill generateCancellationFeeBill(Appointment appointment) {

        AppointmentTypeEnum appointmentType = appointment.getType();
        BillTypeEnum baseBillType = appointmentBillMap.get(appointmentType);
        BigDecimal amount = baseFeeMap.get(baseBillType)
            .multiply(CANCELLATION_FEE_PERCENTAGE)
            .setScale(2, RoundingMode.DOWN);

        BillTypeEnum cancelBillType = appointmentCancelBillMap.get(appointmentType);
        if (cancelBillType==null) throw new IllegalArgumentException();


        return appointmentBillRepository.save(
            createAppointmentBill(appointment, cancelBillType, amount)
        );
    }

    @Override
    @Transactional
    public LabBill generateBillForLabTest(LabTest labTest) {

        if (!LabTestStatusEnum.COMPLETED.equals(labTest.getStatus())) throw new IllegalArgumentException("Cannot create bill for uncompleted lab test");

        LabBill bill = new LabBill();
        bill.setAmount(LAB_TEST_FEE);
        bill.setPatientPaymentAmount(LAB_TEST_FEE);
        bill.setStatus(BillStatusEnum.VIEWING);
        bill.setType(BillTypeEnum.LAB_TEST_FEE);
        bill.setPatient(labTest.getPatient());
        bill.setLabTest(labTest);
        return labBillRepository.save(bill);
    }

    private AppointmentBill createAppointmentBill(Appointment appointment, BillTypeEnum billType, BigDecimal amount){
        AppointmentBill bill = new AppointmentBill();
        bill.setAmount(amount);
        bill.setPatientPaymentAmount(amount);
        bill.setStatus(BillStatusEnum.VIEWING);
        bill.setType(billType);
        bill.setPatient(appointment.getPatient());
        bill.setAppointment(appointment);

        return bill;
    }
}
