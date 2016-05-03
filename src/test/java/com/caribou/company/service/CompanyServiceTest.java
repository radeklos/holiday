package com.caribou.company.service;

import com.caribou.WebApplication;
import com.caribou.company.domain.Company;
import com.caribou.company.repository.CompanyRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import rx.observers.TestSubscriber;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WebApplication.class)
@WebAppConfiguration
public class CompanyServiceTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyService companyService;

    @After
    public void tearDown() throws Exception {
        companyRepository.deleteAll();
    }

    @Test
    public void create() throws Exception {
        TestSubscriber<Company> testSubscriber = new TestSubscriber<>();

        companyService.create(new Company("name", 10)).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        Company company = testSubscriber.getOnNextEvents().get(0);
        Assert.assertNotNull(company.getUid());
    }

    @Test
    public void update() throws Exception {
        Company company = new Company("name", 10);
        companyRepository.save(company);

        TestSubscriber<Company> testSubscriber = new TestSubscriber<>();

        companyService.update(company.getUid(), new Company("new name", 20)).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        Company updated = testSubscriber.getOnNextEvents().get(0);
        Assert.assertEquals(company.getUid(), updated.getUid());
        Assert.assertEquals("new name", updated.getName());
        Assert.assertEquals(new Integer(20), updated.getDefaultDaysOf());
    }

    @Test
    public void updateNonExistingObject() throws Exception {
        TestSubscriber<Company> testSubscriber = new TestSubscriber<>();
        companyService.update(0l, new Company("new name", 20)).subscribe(testSubscriber);
        testSubscriber.assertError(NotFound.class);
    }

}
