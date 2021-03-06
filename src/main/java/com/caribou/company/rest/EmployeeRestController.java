package com.caribou.company.rest;


import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.jwt.UserContext;
import com.caribou.auth.service.UserService;
import com.caribou.company.domain.CompanyEmployee;
import com.caribou.company.domain.Department;
import com.caribou.company.domain.Role;
import com.caribou.company.rest.dto.DepartmentReadDto;
import com.caribou.company.rest.dto.DepartmentWriteDto;
import com.caribou.company.rest.dto.EmployeeDto;
import com.caribou.company.service.CompanyService;
import com.caribou.company.service.DepartmentService;
import com.caribou.company.service.EmployeeService;
import com.caribou.company.service.NotFound;
import com.caribou.holiday.rest.dto.ListDto;
import ma.glasnost.orika.MapperFacade;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


@RestController
@RequestMapping("/v1/companies/{companyUid}/departments")
public class DepartmentRestController {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MapperFacade mapperFacade;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/{uid}", method = RequestMethod.GET)
    public DepartmentReadDto get(@PathVariable("companyUid") String companyUid, @PathVariable("uid") String uid) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!companyUid.equals(userDetails.getCompanyId().toString())) {
            throw new NotFound();
        }
        return convert(departmentService.get(uid));
    }

    private DepartmentReadDto convert(Department entity) {
        DepartmentReadDto departmentDto = mapperFacade.map(entity, DepartmentReadDto.class);
        departmentDto.add(linkTo(methodOn(DepartmentRestController.class).employee(entity.getCompany().getUid().toString(), entity.getUid().toString())).withRel("employees"));
        return departmentDto;
    }

    @RequestMapping(value = "/{departmentUid}/employees")
    public ListDto<EmployeeDto> employee(@PathVariable("companyUid") String companyUid, @PathVariable("departmentUid") String departmentUid) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!userDetails.getCompanyId().toString().equals(companyUid)) {
            throw new NotFound();
        }
        Department department = departmentService.get(departmentUid);
        if (!department.getCompany().getUid().toString().equals(companyUid)) {
            throw new NotFound();
        }
        List<EmployeeDto> employee = departmentService.getEmployees(departmentUid).stream()
                .map(e -> mapperFacade.map(e, EmployeeDto.class))
                .collect(Collectors.toList());
        return ListDto.<EmployeeDto>builder()
                .total(employee.size())
                .items(employee)
                .build();
    }

    @RequestMapping(value = "/{uid}/employees", method = RequestMethod.POST)
    public ResponseEntity createEmployee(@PathVariable("companyUid") String companyUid, @PathVariable("uid") String uid, @Valid @RequestBody EmployeeDto employeeDto) {
        isNotUserViewerInCompany(companyUid);
        employeeService.createEmployee(convert(employeeDto), departmentService.get(uid), employeeDto.getStartedAt());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @RequestMapping(method = RequestMethod.POST)
    public Single<ResponseEntity<DepartmentReadDto>> create(@PathVariable("companyUid") String companyUid, @Valid @RequestBody DepartmentWriteDto departmentDto) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return companyService.getByEmployeeEmail(companyUid, userDetails.getUsername())
                .map(company1 -> {
                    for (CompanyEmployee f : company1.getCompany().getEmployees()) {
                        if (f.getMember().getEmail().equals(userDetails.getUsername()) && f.getRole() == Role.Viewer) {
                            throw new AccessDeniedException("omg");
                        }
                    }
                    return company1;
                })
                .flatMap(employee -> {
                    Department entity = convert(departmentDto);
                    entity.setCompany(employee.getCompany());
                    return departmentService.create(entity);
                })
                .map(d -> new ResponseEntity<>(convert(d), HttpStatus.CREATED))
                .onErrorReturn(ErrorHandler::h)
                .toSingle();
    }

    private Department convert(DepartmentWriteDto dto) {
        Department department = modelMapper.map(dto, Department.class);
        Optional<CompanyEmployee> boss = companyService.findEmployeeByUid(dto.getBoss());
        if (!boss.isPresent()) {
            throw new NotFound();
        }
        department.setBoss(boss.get().getMember());
        return department;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ListDto<DepartmentReadDto> getList(@PathVariable("companyUid") String companyUid) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!companyUid.equals(userDetails.getCompanyId().toString())) {
            throw new NotFound();
        }
        List<Department> departments = departmentService.getDepartments(companyUid);
        return ListDto.<DepartmentReadDto>builder()
                .total(departments.size())
                .items(convert(departments))
                .build();
    }

    private List<DepartmentReadDto> convert(List<Department> dtos) {
        return dtos.stream().map(this::convert).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{uid}", method = RequestMethod.PUT)
    public Single<DepartmentWriteDto> update(@PathVariable("companyUid") String companyUid, @PathVariable("uid") String uid, @Valid @RequestBody DepartmentWriteDto departmentDto) {
        // TODO acl
        return companyService.getRx(companyUid)
                .flatMap(company -> {
                    Department entity = convert(departmentDto);
                    entity.setCompany(company);
                    return departmentService.update(uid, entity);
                })
                .map(d -> {
                    modelMapper.map(d, departmentDto);
                    return departmentDto;
                }).toSingle();
    }

    private UserContext isNotUserViewerInCompany(String companyUid) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!companyUid.equals(userDetails.getCompanyId().toString())) {
            throw new NotFound();
        }
        if (Role.Viewer.equals(userDetails.getRoleInCompany())) {
            throw new AccessDeniedException("user={} doesn't have permission to pass though" + userDetails.getUid().toString());
        }
        return userDetails;
    }

    private UserAccount convert(EmployeeDto employeeDto) {
        return mapperFacade.map(employeeDto, UserAccount.class);
    }

}
