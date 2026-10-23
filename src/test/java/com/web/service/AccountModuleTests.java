package com.web.service;

import com.web.controller.admin.UserAdminController;
import com.web.entity.User;
import com.web.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountModuleTests {
    private UserAdminController controller;
    private UserService service;
    @BeforeEach void init() {
        controller = new UserAdminController(); service = mock(UserService.class);
        ReflectionTestUtils.setField(controller,"userService",service);
        when(service.getAllUser(any(),any(),any(),any(),any(),any())).thenReturn(Page.empty());
    }
    @Test void employeeListUsesDedicatedTitleAndDatabaseStatusFilter() {
        var model = new ExtendedModelMap();
        controller.viewPage(model,Role.ROLE_EMPLOYEE,false,null,null,5,"staff",PageRequest.of(0,5));
        assertEquals("Danh sách nhân viên",model.get("accountTitle"));
        verify(service).getAllUser(eq(Role.ROLE_EMPLOYEE),isNull(),isNull(),eq("staff"),eq(false),any());
    }
    @Test void customerListUsesDedicatedTitle() {
        var model = new ExtendedModelMap();
        controller.viewPage(model,Role.ROLE_USER,true,null,null,5,"",PageRequest.of(0,5));
        assertEquals("Danh sách khách hàng",model.get("accountTitle"));
        assertEquals("Thêm khách hàng",model.get("addAccountLabel"));
    }
    @Test void newStaffFormDefaultsToSelectedRole() {
        var model = new ExtendedModelMap(); controller.showCreateForm(model,null,Role.ROLE_EMPLOYEE);
        assertEquals(Role.ROLE_EMPLOYEE,((User)model.get("user")).getRole());
    }
    @Test void invalidPageSizeIsBadRequestBeforeDatabaseQuery() {
        var error = assertThrows(ResponseStatusException.class,()->controller.viewPage(new ExtendedModelMap(),null,null,null,null,0,"",PageRequest.of(0,5)));
        assertEquals(400,error.getStatusCode().value()); verifyNoInteractions(service);
    }
}
