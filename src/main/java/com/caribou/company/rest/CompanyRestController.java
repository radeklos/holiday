package com.caribou.company.rest;


import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.jwt.UserContext;
import com.caribou.auth.service.UserService;
import com.caribou.company.Pair;
import com.caribou.company.domain.Company;
import com.caribou.company.domain.CompanyEmployee;
import com.caribou.company.domain.Role;
import com.caribou.company.rest.dto.CompanyDto;
import com.caribou.company.rest.dto.EmployeeDto;
import com.caribou.company.service.CompanyService;
import com.caribou.company.service.EmployeeService;
import com.caribou.company.service.NotFound;
import com.caribou.company.service.parser.EmployeeCsvParser;
import com.caribou.holiday.rest.dto.ListDto;
import ma.glasnost.orika.MapperFacade;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


@RestController
@RequestMapping("/v1/companies")
public class CompanyRestController {

    private final CompanyService companyService;
    private final UserService userService;
    private final EmployeeCsvParser employeeCsvParser;
    private final EmployeeService employeeService;
    private ModelMapper modelMapper = new ModelMapper();
    private final MapperFacade mapperFacade;

    @Autowired
    public CompanyRestController(CompanyService companyService, UserService userService, EmployeeCsvParser employeeCsvParser, EmployeeService employeeService, MapperFacade mapperFacade) {
        this.companyService = companyService;
        this.userService = userService;
        this.employeeCsvParser = employeeCsvParser;
        this.employeeService = employeeService;
        this.mapperFacade = mapperFacade;
    }

    private static ResponseEntity errorHandler(Throwable throwable) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (throwable instanceof EmployeeService.DepartmentNotFound) {
            return new ResponseEntity(headers, HttpStatus.BAD_REQUEST);
        }
        return ErrorHandler.h(throwable);
    }

    @RequestMapping(value = "/{uid}", method = RequestMethod.GET)
    public Single<CompanyDto> get(@PathVariable("uid") String uid) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return companyService.getByEmployeeEmail(uid, userDetails.getUsername())
                .map(c -> convert(c.getCompany()))
                .toSingle();
    }

    private CompanyDto convert(Company company) {
        CompanyDto companyDto = modelMapper.map(company, CompanyDto.class);
        companyDto.add(linkTo(methodOn(DepartmentRestController.class).getList(company.getUid().toString())).withRel("department"));
        return companyDto;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Single<ResponseEntity<CompanyDto>> create(@Valid @RequestBody CompanyDto newCompany) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserAccount loggedUser = userService.findByEmail(userDetails.getUsername()).toBlocking().first();

        Company company = convert(newCompany);
        company.addEmployee(loggedUser, Role.Owner);
        return companyService.create(company)
                .map(d -> new ResponseEntity<>(convert(d), HttpStatus.CREATED))
                .toSingle();
    }

    private Company convert(CompanyDto newCompany) {
        return modelMapper.map(newCompany, Company.class);
    }

    @RequestMapping(value = "/{uid}", method = RequestMethod.PUT)
    public Single<CompanyDto> update(@PathVariable("uid") String uid, @Valid @RequestBody CompanyDto companyDto) {
        // TODO some acl
        Company company = convert(companyDto);
        return companyService.update(uid, company)
                .map(this::convert)
                .toSingle();
    }

    @RequestMapping(value = "/{uid}/employees", method = RequestMethod.GET)
    public ListDto<EmployeeDto> getEmployees(@PathVariable("uid") String companyId) {
        UserContext user = getUserForCompanyOr404(companyId);
        List<EmployeeDto> employees = companyService.findEmployeesByCompanyUid(user.getCompanyId()).stream()
                .map(this::map)
                .collect(Collectors.toList());
        return ListDto.<EmployeeDto>builder()
                .items(employees)
                .total(employees.size())
                .build();
    }

    @RequestMapping(value = "/{uid}/employees", method = RequestMethod.POST)
    public Single<ResponseEntity<Object>> update(@PathVariable("uid") String uid, @RequestParam("file") MultipartFile file) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return companyService.getByEmployeeEmail(uid, userDetails.getUsername())
                .map(employee -> {
                    if (!Arrays.asList(Role.Admin, Role.Editor, Role.Owner).contains(employee.getRole())) {
                        throw new AccessDeniedException("Role " + employee.getRole() + " can not import employees");
                    }
                    return employee.getCompany();
                })
                .flatMap(company -> Observable.create((Observable.OnSubscribe<Pair<Company, List<EmployeeCsvParser.Row>>>) subscriber -> {
                    try {
                        subscriber.onNext(new Pair<>(company, employeeCsvParser.read(file).readAll()));
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }))
                .flatMap(u -> employeeService.importEmployee(u.second, u.first))
                .map(employeeService::sendInvitationEmail)
                .toList()
                .map(u -> ResponseEntity.accepted().build())
                .onErrorReturn(CompanyRestController::errorHandler)
                .toSingle();
    }

    @RequestMapping(value = "/examples/employees", method = RequestMethod.GET)
    public HttpEntity<byte[]> example() {
        byte[] documentBody = employeeCsvParser.generateExample().getBytes();
        HttpHeaders header = new HttpHeaders();
        header.set(HttpHeaders.CONTENT_TYPE, "text/csv");
        header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=example.csv");
        header.setContentLength(documentBody.length);
        return new HttpEntity<>(documentBody, header);
    }

    private UserContext getUserForCompanyOr404(String companyId) {
        UserContext userDetails = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userDetails.getCompanyId() != null && companyId.equals(userDetails.getCompanyId().toString())) {
            return userDetails;
        }
        throw new NotFound();
    }

    private EmployeeDto map(CompanyEmployee companyEmployee) {
        return mapperFacade.map(companyEmployee, EmployeeDto.class);
    }

}
