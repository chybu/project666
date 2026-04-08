package com.project666.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.ListBillRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.LabTestStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.domain.message.BillReadyForInsuranceEvent;
import com.project666.backend.repository.AppointmentBillRepository;
import com.project666.backend.repository.LabBillRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.BillService;
import com.project666.backend.specification.AppointmentBillSpecification;
import com.project666.backend.specification.BaseBillSpecification;
import com.project666.backend.specification.LabBillSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService{

    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

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

        AppointmentBill saved = appointmentBillRepository.save(
            createAppointmentBill(appointment, billType, amount)
        );
        applicationEventPublisher.publishEvent(
            new BillReadyForInsuranceEvent(saved.getId())
        );
        return saved;
    }

    @Override
    @Transactional
    public AppointmentBill generateLateFeeBill(Appointment appointment) {
        
        AppointmentBill saved = appointmentBillRepository.save(
            createAppointmentBill(appointment, BillTypeEnum.LATE_FEE, LATE_FEE_AMOUNT)
        );
        applicationEventPublisher.publishEvent(
            new BillReadyForInsuranceEvent(saved.getId())
        );
        return saved;
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

        AppointmentBill saved = appointmentBillRepository.save(
            createAppointmentBill(appointment, cancelBillType, amount)
        );
        applicationEventPublisher.publishEvent(
            new BillReadyForInsuranceEvent(saved.getId())
        );
        return saved;
    }

    @Override
    @Transactional
    public LabBill generateBillForLabTest(LabTest labTest) {

        if (!LabTestStatusEnum.COMPLETED.equals(labTest.getStatus())) throw new IllegalArgumentException("Cannot create bill for uncompleted lab test");

        LabBill bill = new LabBill();
        bill.setAmount(LAB_TEST_FEE);
        bill.setPatientPaymentAmount(LAB_TEST_FEE);
        bill.setInsuranceCoverAmount(BigDecimal.ZERO);
        bill.setStatus(BillStatusEnum.VIEWING);
        bill.setType(BillTypeEnum.LAB_TEST_FEE);
        bill.setPatient(labTest.getPatient());
        bill.setLabTest(labTest);

        LabBill saved = labBillRepository.save(bill);
        applicationEventPublisher.publishEvent(
            new BillReadyForInsuranceEvent(saved.getId())
        );
        return saved;
    }

    @Override
    public Page<AppointmentBill> listAppointmentBillForPatient(
        UUID patientId,
        ListAppointmentBillRequest request,
        Pageable pageable
    ) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.ACCOUNTANT, request.getConfirmAccountantId());
        return listAppointmentBillHelper(request, roleMap, pageable);
    }

    @Override
    public Page<AppointmentBill> listAppointmentBillForAccountant(
        UUID accountantId,
        ListAppointmentBillRequest request,
        Pageable pageable
    ) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.ACCOUNTANT, accountantId);
        return listAppointmentBillHelper(request, roleMap, pageable);
    }

    @Override
    public Page<AppointmentBill> searchAnyAppointmentBillForAccountant(
        UUID accountantId,
        ListAppointmentBillRequest request,
        Pageable pageable
    ) {
        userRepository.findByIdAndRole(accountantId, RoleEnum.ACCOUNTANT)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("ACCOUNTANT with ID %s not found", accountantId)
            )
        );

        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.ACCOUNTANT, request.getConfirmAccountantId());
        return listAppointmentBillHelper(request, roleMap, pageable);
    }

    @Override
    public Page<LabBill> listLabBillForPatient(
        UUID patientId,
        ListLabBillRequest request,
        Pageable pageable
    ) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.ACCOUNTANT, request.getConfirmAccountantId());
        return listLabBillHelper(request, roleMap, pageable);
    }

    @Override
    public Page<LabBill> listLabBillForAccountant(
        UUID accountantId,
        ListLabBillRequest request,
        Pageable pageable
    ) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.ACCOUNTANT, accountantId);
        return listLabBillHelper(request, roleMap, pageable);
    }

    @Override
    public Page<LabBill> searchAnyLabBillForAccountant(
        UUID accountantId,
        ListLabBillRequest request,
        Pageable pageable
    ) {
        userRepository.findByIdAndRole(accountantId, RoleEnum.ACCOUNTANT)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("ACCOUNTANT with ID %s not found", accountantId)
            )
        );

        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.ACCOUNTANT, request.getConfirmAccountantId());
        return listLabBillHelper(request, roleMap, pageable);
    }

    @Override
    @Transactional
    public BaseBill confirmBillPayment(UUID accountantId, UUID billId) {
        LocalDateTime paidTime = LocalDateTime.now();

        User accountant = userRepository.findByIdAndRole(accountantId, RoleEnum.ACCOUNTANT)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("ACCOUNTANT with ID %s not found", accountantId)
            ));

        BillStatusEnum currentBillStatus;

        AppointmentBill appointmentBill = appointmentBillRepository.findById(billId).orElse(null);
        if (appointmentBill != null) {
            currentBillStatus = appointmentBill.getStatus();
            if (BillStatusEnum.PAID.equals(currentBillStatus)) {
                throw new IllegalArgumentException(
                    String.format("Bill with ID %s is already paid", billId)
                );
            }
            if (BillStatusEnum.VIEWING.equals(currentBillStatus)) {
                throw new IllegalArgumentException(
                    String.format("Bill with ID %s is being viewed by Insurance Service", billId)
                );
            }

            appointmentBill.setStatus(BillStatusEnum.PAID);
            appointmentBill.setConfirmAccountant(accountant);
            appointmentBill.setPaidOn(paidTime);
            return appointmentBillRepository.save(appointmentBill);
        }

        LabBill labBill = labBillRepository.findById(billId).orElseThrow(() ->
            new NoSuchElementException(
                String.format("Bill with ID %s not found", billId)
            )
        );

        currentBillStatus = labBill.getStatus();
        if (BillStatusEnum.PAID.equals(currentBillStatus)) {
            throw new IllegalArgumentException(
                String.format("Bill with ID %s is already paid", billId)
            );
        }

        if (BillStatusEnum.VIEWING.equals(currentBillStatus)) {
                throw new IllegalArgumentException(
                    String.format("Bill with ID %s is being viewed by Insurance Service", billId)
                );
        }

        labBill.setStatus(BillStatusEnum.PAID);
        labBill.setConfirmAccountant(accountant);
        labBill.setPaidOn(paidTime);
        return labBillRepository.save(labBill);
    }

    private <T extends BaseBill> Specification<T> buildBaseBillSpecification(
        ListBillRequest request,
        Map<RoleEnum, UUID> roleMap
    ) {
        roleMap.forEach((role, id) -> {
            if (id != null) {
                userRepository.findByIdAndRole(id, role).orElseThrow(() ->
                    new NoSuchElementException(
                        String.format("%s with ID %s not found", role.name(), id)
                    )
                );
            }
        });

        validateBillRange(request);

        Specification<T> spec = BaseBillSpecification.<T>alwaysTrue();

        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId != null) {
            spec = spec.and(BaseBillSpecification.byPatient(patientId));
        }

        UUID accountantId = roleMap.get(RoleEnum.ACCOUNTANT);
        if (accountantId != null) {
            spec = spec.and(BaseBillSpecification.byConfirmAccountant(accountantId));
        }

        BillStatusEnum status = request.getStatus();
        if (status != null) {
            spec = spec.and(BaseBillSpecification.byStatus(status));
        }

        BillTypeEnum type = request.getType();
        if (type != null) {
            spec = spec.and(BaseBillSpecification.byType(type));
        }

        if (request.getMinAmount() != null || request.getMaxAmount() != null) {
            spec = spec.and(
                BaseBillSpecification.byAmountRange(
                    request.getMinAmount(),
                    request.getMaxAmount()
                )
            );
        }

        if (request.getMinInsuranceCoverAmount() != null || request.getMaxInsuranceCoverAmount() != null) {
            spec = spec.and(
                BaseBillSpecification.byInsuranceCoverAmountRange(
                    request.getMinInsuranceCoverAmount(),
                    request.getMaxInsuranceCoverAmount()
                )
            );
        }

        if (request.getMinPatientPaymentAmount() != null || request.getMaxPatientPaymentAmount() != null) {
            spec = spec.and(
                BaseBillSpecification.byPatientPaymentAmountRange(
                    request.getMinPatientPaymentAmount(),
                    request.getMaxPatientPaymentAmount()
                )
            );
        }

        if (request.getPaidOnDate() != null) {
            spec = spec.and(
                BaseBillSpecification.byPaidOnDate(request.getPaidOnDate())
            );
        }

        if (request.getCreatedAtDate() != null) {
            spec = spec.and(
                BaseBillSpecification.byCreatedAtDate(request.getCreatedAtDate())
            );
        }

        return spec;
    }

    private Page<AppointmentBill> listAppointmentBillHelper(
        ListAppointmentBillRequest request,
        Map<RoleEnum, UUID> roleMap,
        Pageable pageable
    ) {
        Specification<AppointmentBill> spec =
            buildBaseBillSpecification(request, roleMap);

        UUID appointmentId = request.getAppointmentId();
        if (appointmentId != null) {
            spec = spec.and(AppointmentBillSpecification.byAppointment(appointmentId));
        }

        return appointmentBillRepository.findAll(spec, pageable);
    }

    private Page<LabBill> listLabBillHelper(
        ListLabBillRequest request,
        Map<RoleEnum, UUID> roleMap,
        Pageable pageable
    ) {
        Specification<LabBill> spec =
            buildBaseBillSpecification(request, roleMap);

        UUID labTestId = request.getLabTestId();
        if (labTestId != null) {
            spec = spec.and(LabBillSpecification.byLabTest(labTestId));
        }

        return labBillRepository.findAll(spec, pageable);
    }

    private void validateBillRange(ListBillRequest request) {
        if (request.getMinAmount() != null && request.getMaxAmount() != null
            && request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("minAmount must be less than or equal to maxAmount");
        }

        if (request.getMinInsuranceCoverAmount() != null && request.getMaxInsuranceCoverAmount() != null
            && request.getMinInsuranceCoverAmount().compareTo(request.getMaxInsuranceCoverAmount()) > 0) {
            throw new IllegalArgumentException("minInsuranceCoverAmount must be less than or equal to maxInsuranceCoverAmount");
        }

        if (request.getMinPatientPaymentAmount() != null && request.getMaxPatientPaymentAmount() != null
            && request.getMinPatientPaymentAmount().compareTo(request.getMaxPatientPaymentAmount()) > 0) {
            throw new IllegalArgumentException("minPatientPaymentAmount must be less than or equal to maxPatientPaymentAmount");
        }
    }

    private AppointmentBill createAppointmentBill(Appointment appointment, BillTypeEnum billType, BigDecimal amount){
        AppointmentBill bill = new AppointmentBill();
        bill.setAmount(amount);
        bill.setPatientPaymentAmount(amount);
        bill.setInsuranceCoverAmount(BigDecimal.ZERO);
        bill.setStatus(BillStatusEnum.VIEWING);
        bill.setType(billType);
        bill.setPatient(appointment.getPatient());
        bill.setAppointment(appointment);

        return bill;
    }
}
