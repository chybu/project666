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
import com.project666.backend.exception.InvalidAppointmentTypeException;
import com.project666.backend.repository.BillRepository;
import com.project666.backend.service.BillService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService{

    private final BigDecimal CANCELLATION_FEE_PERCENTAGE = new BigDecimal("0.05");
    private final BigDecimal LATE_FEE_AMOUNT = new BigDecimal("10.00");

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

    private final BillRepository billRepository;

    @Override
    @Transactional
    public AppointmentBill generateBillForAppointment(Appointment appointment) {

        AppointmentTypeEnum appointmentType = appointment.getType();
        BillTypeEnum billType = appointmentBillMap.get(appointmentType);
        if (billType == null) throw new InvalidAppointmentTypeException();
        BigDecimal amount = baseFeeMap.get(billType);

        return billRepository.save(
            createAppointmentBill(appointment, billType, amount)
        );
    }

    @Override
    @Transactional
    public AppointmentBill generateLateFeeBill(Appointment appointment) {
        return billRepository.save(
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
        if (cancelBillType==null) throw new InvalidAppointmentTypeException();


        return billRepository.save(
            createAppointmentBill(appointment, cancelBillType, amount)
        );
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
