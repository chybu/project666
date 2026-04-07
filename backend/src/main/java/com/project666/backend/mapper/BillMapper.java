package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.dto.ListAppointmentBillRequestDto;
import com.project666.backend.domain.dto.ListBillResponseDto;
import com.project666.backend.domain.dto.ListLabBillRequestDto;
import com.project666.backend.domain.entity.BaseBill;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BillMapper {

    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "confirmAccountantFullName", source = "confirmAccountant.fullName")
    ListBillResponseDto toListBillResponseDto(BaseBill bill);

    ListAppointmentBillRequest fromListAppointmentBillRequestDto(ListAppointmentBillRequestDto dto);

    ListLabBillRequest fromListLabBillRequestDto(ListLabBillRequestDto dto);
}
